package com.stayhub.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.jayway.jsonpath.JsonPath;
import com.stayhub.common.PropertyScope;
import com.stayhub.content.service.PublicContentService;
import com.stayhub.reservation.entity.ReservationStatus;
import com.stayhub.reservation.repository.ReservationRepository;
import com.stayhub.reservation.service.ReservationService;
import com.stayhub.room.entity.Room;
import com.stayhub.room.repository.RoomRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게스트 예약 API·화면 테스트 (CMS-008, SCR-GST-03). 모든 요청은 로그인 없이 보냅니다.
 *
 * <p>개발 DB 에 다른 객실이 있어도 흔들리지 않도록, "내가 만든 객실이 목록에 있나/없나"만 봅니다.
 * 대체 날짜 테스트는 최대 인원 999명 객실을 만들어 그 객실만 조건에 맞게 합니다.
 * {@code @Transactional} 이라 끝나면 롤백됩니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PublicReservationApiTest {

	private static final LocalDate DAY = LocalDate.now().plusDays(200);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RoomRepository roomRepository;

	@Autowired
	private ReservationService reservationService;

	@Autowired
	private ReservationRepository reservationRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private String tag;

	@BeforeEach
	void setUp() {
		tag = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
		saveProperty("PUBLISHED");
	}

	/** 게스트 경로는 숙소(property_id = 1)가 PUBLISHED 일 때만 열립니다. PublicContentApiTest.saveProperty 와 같은 방식. */
	private void saveProperty(String status) {
		jdbcTemplate.update("""
				INSERT INTO properties (property_id, property_name, description, status, created_at, updated_at)
				VALUES (?, '바다 앞 펜션', '소개', ?, NOW(), NOW())
				ON DUPLICATE KEY UPDATE status = VALUES(status)
				""", PropertyScope.DEFAULT_PROPERTY_ID, status);
	}

	private Long room(int maxOccupancy) {
		return roomRepository.save(Room.create(PropertyScope.DEFAULT_PROPERTY_ID, "G" + tag + maxOccupancy,
				"STANDARD", new BigDecimal("100000.00"), maxOccupancy)).getId();
	}

	private void book(Long roomId, LocalDate checkIn, LocalDate checkOut, int guests) {
		reservationService.create(null, "기존 손님", "010-0000-0000", roomId, null, checkIn, checkOut, guests);
	}

	private ResultActions available(LocalDate checkIn, LocalDate checkOut, int guests) throws Exception {
		return mockMvc.perform(get("/api/public/rooms/available")
				.param("checkIn", checkIn.toString())
				.param("checkOut", checkOut.toString())
				.param("guests", String.valueOf(guests)));
	}

	private String json(Long roomId, LocalDate checkIn, LocalDate checkOut, int guests) {
		return """
				{"roomId":%d,"checkIn":"%s","checkOut":"%s","guestCount":%d,"guestName":"홍길동","guestPhone":"010-1234-5678"}
				""".formatted(roomId, checkIn, checkOut, guests);
	}

	@Test
	@DisplayName("로그인 없이 빈 객실 조회가 200 이고, 가격은 1박 × 박수, 내부 값은 없다")
	void availableWithoutLogin() throws Exception {
		Long roomId = room(4);

		available(DAY, DAY.plusDays(2), 2)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.rooms[?(@.roomId == %d)].roomNumber", roomId).value(Matchers.contains("G" + tag + 4)))
				.andExpect(jsonPath("$.data.rooms[?(@.roomId == %d)].pricePerNight", roomId).value(Matchers.contains(100000.0)))
				.andExpect(jsonPath("$.data.rooms[?(@.roomId == %d)].totalPrice", roomId).value(Matchers.contains(200000.0)))
				.andExpect(jsonPath("$.data.rooms[0].status").doesNotExist())
				.andExpect(jsonPath("$.data.rooms[0].version").doesNotExist())
				.andExpect(jsonPath("$.data.alternativeDates").isEmpty());
	}

	@Test
	@DisplayName("날짜가 겹치는 객실은 빠지고, 체크아웃 날 체크인은 겹치지 않으며, 취소된 예약은 막지 않는다")
	void overlappingRoomExcluded() throws Exception {
		Long roomId = room(4);
		book(roomId, DAY, DAY.plusDays(2), 2);

		available(DAY.plusDays(1), DAY.plusDays(3), 2)
				.andExpect(jsonPath("$.data.rooms[*].roomId").value(Matchers.not(Matchers.hasItem(roomId.intValue()))));
		available(DAY.plusDays(2), DAY.plusDays(3), 2)
				.andExpect(jsonPath("$.data.rooms[*].roomId").value(Matchers.hasItem(roomId.intValue())));

		jdbcTemplate.update("UPDATE reservations SET status = 'CANCELLED' WHERE room_id = ?", roomId);
		available(DAY.plusDays(1), DAY.plusDays(3), 2)
				.andExpect(jsonPath("$.data.rooms[*].roomId").value(Matchers.hasItem(roomId.intValue())));
	}

	@Test
	@DisplayName("인원이 최대 인원을 넘는 객실은 빠진다")
	void overCapacityExcluded() throws Exception {
		Long roomId = room(2);

		available(DAY, DAY.plusDays(1), 3)
				.andExpect(jsonPath("$.data.rooms[*].roomId").value(Matchers.not(Matchers.hasItem(roomId.intValue()))));
		available(DAY, DAY.plusDays(1), 2)
				.andExpect(jsonPath("$.data.rooms[*].roomId").value(Matchers.hasItem(roomId.intValue())));
	}

	@Test
	@DisplayName("과거 체크인·31박·체크아웃 ≤ 체크인·인원 0 은 400, 30박은 된다")
	void invalidSearch() throws Exception {
		LocalDate today = LocalDate.now();
		available(today.minusDays(1), today.plusDays(1), 1).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("checkIn: 체크인 날짜는 오늘 이후로 선택해 주세요."));
		available(today, today.plusDays(31), 1).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("checkOut: 최대 30박까지 예약할 수 있습니다."));
		available(today, today, 1).andExpect(status().isBadRequest());
		available(today, today.plusDays(1), 0).andExpect(status().isBadRequest());
		available(today, today.plusDays(30), 1).andExpect(status().isOk());
	}

	@Test
	@DisplayName("빈 객실이 없으면 같은 박수로 실제로 비어 있는 첫 날짜 3개를 대체 날짜로 준다")
	void alternativeDatesAreReallyFree() throws Exception {
		Long roomId = room(999); // 999명 객실은 이것 하나뿐 → 이 객실이 비는 날짜만 대체 날짜가 됨
		book(roomId, DAY, DAY.plusDays(3), 1);

		String body = available(DAY, DAY.plusDays(2), 999)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.rooms").isEmpty())
				.andReturn().getResponse().getContentAsString();

		// DAY+1(~DAY+3), DAY+2(~DAY+4) 는 기존 예약과 겹치고, DAY+3 부터 비어 있음
		List<Map<String, String>> alternatives = JsonPath.read(body, "$.data.alternativeDates");
		assertThat(alternatives).extracting(d -> d.get("checkIn"))
				.containsExactly(DAY.plusDays(3).toString(), DAY.plusDays(4).toString(), DAY.plusDays(5).toString());

		for (Map<String, String> d : alternatives) {
			LocalDate in = LocalDate.parse(d.get("checkIn"));
			LocalDate out = LocalDate.parse(d.get("checkOut"));
			assertThat(out).isEqualTo(in.plusDays(2));
			assertThat(reservationRepository.existsOverlap(roomId, in, out, ReservationStatus.RELEASED)).isFalse();
			available(in, out, 999).andExpect(jsonPath("$.data.rooms[*].roomId").value(Matchers.contains(roomId.intValue())));
		}
	}

	@Test
	@DisplayName("로그인 없이 예약 생성 → CONFIRMED, channel_id·user_id NULL, 응답에 내부 값 없음")
	void createWithoutLogin() throws Exception {
		Long roomId = room(4);

		String body = mockMvc.perform(post("/api/public/reservations")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(roomId, DAY, DAY.plusDays(3), 2)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.reservationCode").value(Matchers.startsWith("R")))
				.andExpect(jsonPath("$.data.roomNumber").value("G" + tag + 4))
				.andExpect(jsonPath("$.data.checkIn").value(DAY.toString()))
				.andExpect(jsonPath("$.data.checkOut").value(DAY.plusDays(3).toString()))
				.andExpect(jsonPath("$.data.guestCount").value(2))
				.andExpect(jsonPath("$.data.totalPrice").value(300000.0))
				.andExpect(jsonPath("$.data.id").doesNotExist())
				.andExpect(jsonPath("$.data.status").doesNotExist())
				.andExpect(jsonPath("$.data.guestPhone").doesNotExist())
				.andReturn().getResponse().getContentAsString();

		String code = JsonPath.read(body, "$.data.reservationCode");
		Map<String, Object> row = jdbcTemplate.queryForMap(
				"SELECT status, channel_id, user_id, guest_name FROM reservations WHERE reservation_code = ?", code);
		assertThat(row.get("status")).isEqualTo("CONFIRMED");
		assertThat(row.get("channel_id")).isNull();
		assertThat(row.get("user_id")).isNull();
		assertThat(row.get("guest_name")).isEqualTo("홍길동");
	}

	@Test
	@DisplayName("이미 예약된 객실·날짜로 생성하면 거부, 과거 날짜·인원 초과·이름 누락도 거부")
	void createRejected() throws Exception {
		Long roomId = room(2);
		book(roomId, DAY, DAY.plusDays(2), 1);

		mockMvc.perform(post("/api/public/reservations").contentType(MediaType.APPLICATION_JSON)
						.content(json(roomId, DAY.plusDays(1), DAY.plusDays(2), 1)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("선택한 날짜에 이미 예약된 객실입니다."));
		mockMvc.perform(post("/api/public/reservations").contentType(MediaType.APPLICATION_JSON)
						.content(json(roomId, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1), 1)))
				.andExpect(status().isBadRequest());
		mockMvc.perform(post("/api/public/reservations").contentType(MediaType.APPLICATION_JSON)
						.content(json(roomId, DAY.plusDays(5), DAY.plusDays(6), 3)))
				.andExpect(status().isBadRequest());
		mockMvc.perform(post("/api/public/reservations").contentType(MediaType.APPLICATION_JSON)
						.content(json(roomId, DAY.plusDays(5), DAY.plusDays(6), 1).replace("홍길동", " ")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("guestName: 예약자 이름을 입력해 주세요."));
	}

	@Test
	@DisplayName("숙소가 DRAFT 면 빈 객실 조회는 '준비 중인 숙소입니다.'")
	void draftPropertyBlocksAvailability() throws Exception {
		room(4);
		saveProperty("DRAFT");

		available(DAY, DAY.plusDays(1), 1)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(PublicContentService.NOT_READY_MESSAGE));
	}

	@Test
	@DisplayName("숙소가 DRAFT 면 예약 생성도 '준비 중인 숙소입니다.' 이고 reservations 행이 늘지 않는다")
	void draftPropertyBlocksCreate() throws Exception {
		Long roomId = room(4);
		saveProperty("DRAFT");
		long before = reservationRepository.count();

		mockMvc.perform(post("/api/public/reservations").contentType(MediaType.APPLICATION_JSON)
						.content(json(roomId, DAY, DAY.plusDays(1), 1)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(PublicContentService.NOT_READY_MESSAGE));

		assertThat(reservationRepository.count()).isEqualTo(before);
	}

	@Test
	@DisplayName("관리자 보호 유지: 로그인 없이 /api/reservations 는 401, /admin 은 /login?expired, /reserve 화면은 200")
	void protectionStays() throws Exception {
		mockMvc.perform(get("/api/reservations")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/admin")).andExpect(redirectedUrl("/login?expired"));
		mockMvc.perform(get("/reserve")).andExpect(status().isOk()).andExpect(view().name("guest/reserve"));
	}
}
