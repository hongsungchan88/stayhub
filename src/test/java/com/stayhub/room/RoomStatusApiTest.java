package com.stayhub.room;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stayhub.account.dto.LoginUser;
import com.stayhub.account.entity.UserRole;
import com.stayhub.room.dto.HousekeepingTaskCreateRequest;
import com.stayhub.room.dto.RoomCreateRequest;
import com.stayhub.room.dto.RoomStatusChangeRequest;
import com.stayhub.room.repository.RoomStatusLogRepository;
import com.stayhub.room.entity.RoomStatus;
import com.stayhub.room.service.HousekeepingTaskService;
import com.stayhub.room.service.RoomService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 객실·청소 작업 상태 변경 API 테스트.
 *
 * <p>담당자 ID 가 요청 본문이 아니라 로그인 세션에서 오는지 확인합니다.
 * 세션은 {@link MockHttpSession} 에 {@link LoginUser} 를 직접 넣어 만듭니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RoomStatusApiTest {

	private static final Long LOGIN_USER_ID = 42L;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RoomService roomService;

	@Autowired
	private HousekeepingTaskService housekeepingTaskService;

	@Autowired
	private RoomStatusLogRepository roomStatusLogRepository;

	private MockHttpSession session;

	@BeforeEach
	void setUp() {
		// 경고: @Transactional을 지우면 개발 DB의 이 테이블이 실제로 비워집니다. 지우지 마세요.
		roomStatusLogRepository.deleteAll();
		session = new MockHttpSession();
		session.setAttribute(LoginUser.SESSION_KEY, new LoginUser(LOGIN_USER_ID, "tester", "테스터", UserRole.HOST));
	}

	@Test
	@DisplayName("객실 상태를 바꾸면 담당자는 로그인 사용자이고, 본문의 changedBy 는 무시된다")
	void roomStatusUsesLoginUser() throws Exception {
		Long roomId = createRoom();

		mockMvc.perform(put("/api/rooms/{id}/status", roomId).session(session)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"previousStatus":"VACANT","newStatus":"OCCUPIED","changeReason":"checkin","changedBy":999}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.statusUpdatedBy").value(LOGIN_USER_ID));

		assertThat(roomStatusLogRepository.findAll()).singleElement()
				.satisfies(log -> assertThat(log.getChangedBy()).isEqualTo(LOGIN_USER_ID));
	}

	@Test
	@DisplayName("청소 작업을 시작하면 객실 담당자와 이력의 담당자가 로그인 사용자다")
	void housekeepingStatusUsesLoginUser() throws Exception {
		Long roomId = createRoom();
		roomService.changeStatus(roomId, new RoomStatusChangeRequest(
				RoomStatus.VACANT, RoomStatus.CLEANING_NEEDED, "체크아웃"), 1L);
		Long taskId = housekeepingTaskService.create(new HousekeepingTaskCreateRequest(
				roomId, null, LocalDateTime.of(2026, 11, 2, 11, 0), null, null, LocalDateTime.of(2026, 11, 2, 12, 0))).id();

		mockMvc.perform(put("/api/housekeeping-tasks/{id}/status", taskId).session(session)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"newStatus":"IN_PROGRESS"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

		assertThat(roomService.getDetail(roomId).statusUpdatedBy()).isEqualTo(LOGIN_USER_ID);
		assertThat(roomStatusLogRepository.findAll())
				.filteredOn(log -> log.getNewStatus() == RoomStatus.CLEANING)
				.singleElement()
				.satisfies(log -> assertThat(log.getChangedBy()).isEqualTo(LOGIN_USER_ID));
	}

	@Test
	@DisplayName("로그인하지 않으면 상태를 바꿀 수 없다")
	void withoutLoginIs401() throws Exception {
		mockMvc.perform(put("/api/rooms/{id}/status", 1L)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"previousStatus":"VACANT","newStatus":"OCCUPIED","changeReason":"checkin"}
								"""))
				.andExpect(status().isUnauthorized());
	}

	private Long createRoom() {
		return roomService.create(new RoomCreateRequest("API-101", "싱글", new BigDecimal("50000.00"), 1)).id();
	}
}
