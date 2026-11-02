package com.stayhub.room;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stayhub.common.BusinessException;
import com.stayhub.common.PageResponse;
import com.stayhub.room.dto.RoomCreateRequest;
import com.stayhub.room.dto.RoomResponse;
import com.stayhub.room.dto.RoomStatusChangeRequest;
import com.stayhub.room.dto.RoomUpdateRequest;
import com.stayhub.room.entity.RoomStatus;
import com.stayhub.room.entity.RoomStatusLog;
import com.stayhub.room.repository.RoomRepository;
import com.stayhub.room.repository.RoomStatusLogRepository;
import com.stayhub.room.service.RoomService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 객실 서비스 테스트.
 *
 * <p>실제 DB(application-local.yml 의 MySQL)에 붙어서 등록·조회·수정이 제대로 되는지 확인합니다.
 * {@code @Transactional} 이 붙어 있어서 테스트가 끝나면 넣었던 데이터가 전부 되돌려집니다(롤백).
 * 유형 ②(업무 상태 있음) 테이블이라 삭제 테스트는 없습니다.
 * 상태 변경(ROM-001)은 room_status_logs 에 이력이 남는지까지 확인합니다.
 */
@SpringBootTest
@Transactional
class RoomServiceTest {

	@Autowired
	private RoomService roomService;

	@Autowired
	private RoomRepository roomRepository;

	@Autowired
	private RoomStatusLogRepository roomStatusLogRepository;

	@BeforeEach
	void setUp() {
		// 개발 DB 에 이미 들어 있는 데이터 때문에 결과가 흔들리지 않도록 테스트 안에서만 비웁니다.
		// @Transactional 이라 테스트가 끝나면 지운 것까지 원래대로 돌아옵니다.
		// 경고: @Transactional을 지우면 개발 DB의 이 테이블이 실제로 비워집니다. 지우지 마세요.
		roomStatusLogRepository.deleteAll();
		roomRepository.deleteAll();
	}

	@Test
	@DisplayName("등록하면 목록에 나온다")
	void createThenAppearsInList() {
		RoomResponse created = roomService.create(createRequest("101"));

		PageResponse<RoomResponse> all = roomService.getList(0);
		assertThat(all.content()).extracting(RoomResponse::id).containsExactly(created.id());

		// 상태는 VACANT, 최대 인원은 비워 보내면 1 로 시작한다
		assertThat(created.status()).isEqualTo(RoomStatus.VACANT);
		assertThat(created.maxOccupancy()).isEqualTo(1);
	}

	@Test
	@DisplayName("수정하면 바뀐 값이 조회된다")
	void updateIsApplied() {
		RoomResponse created = roomService.create(createRequest("101"));

		roomService.update(created.id(), new RoomUpdateRequest(
				"202", "SUITE", new BigDecimal("250000.00"), 4));

		RoomResponse found = roomService.getDetail(created.id());
		assertThat(found.roomNumber()).isEqualTo("202");
		assertThat(found.roomType()).isEqualTo("SUITE");
		assertThat(found.basePrice()).isEqualByComparingTo("250000.00");
		assertThat(found.maxOccupancy()).isEqualTo(4);
	}

	@Test
	@DisplayName("없는 항목을 상세 조회하면 BusinessException 이 난다")
	void getMissingThrows() {
		assertThatThrownBy(() -> roomService.getDetail(99999L))
				.isInstanceOf(BusinessException.class)
				.hasMessage("객실을 찾을 수 없습니다.");
	}

	@Test
	@DisplayName("같은 객실 번호로 등록하면 BusinessException 이 난다")
	void createDuplicateRoomNumberThrows() {
		roomService.create(createRequest("101"));

		assertThatThrownBy(() -> roomService.create(createRequest("101")))
				.isInstanceOf(BusinessException.class)
				.hasMessage("이미 등록된 객실 번호입니다.");
	}

	@Test
	@DisplayName("다른 객실의 번호로 수정하면 BusinessException 이 나고, 자기 번호 그대로는 수정된다")
	void updateDuplicateRoomNumberThrows() {
		roomService.create(createRequest("101"));
		RoomResponse other = roomService.create(createRequest("102"));

		assertThatThrownBy(() -> roomService.update(other.id(), new RoomUpdateRequest(
				"101", "DOUBLE", new BigDecimal("100000.00"), null)))
				.isInstanceOf(BusinessException.class)
				.hasMessage("이미 등록된 객실 번호입니다.");

		// 자기 번호(102)를 그대로 두고 다른 값만 바꾸는 건 허용
		roomService.update(other.id(), new RoomUpdateRequest(
				"102", "SUITE", new BigDecimal("100000.00"), null));
		assertThat(roomService.getDetail(other.id()).roomType()).isEqualTo("SUITE");
	}

	@Test
	@DisplayName("상태를 바꾸면 담당자·시각이 남고, 충돌 아님(false) 이력이 쌓인다")
	void changeStatusIsAppliedAndLogged() {
		RoomResponse created = roomService.create(createRequest("101"));

		RoomResponse changed = roomService.changeStatus(created.id(), new RoomStatusChangeRequest(
				RoomStatus.VACANT, RoomStatus.OCCUPIED, "체크인", 7L));

		assertThat(changed.status()).isEqualTo(RoomStatus.OCCUPIED);
		assertThat(changed.statusUpdatedBy()).isEqualTo(7L);
		assertThat(changed.statusUpdatedAt()).isNotNull();

		assertThat(roomStatusLogRepository.findAll()).singleElement().satisfies(log -> {
			assertThat(log.getRoomId()).isEqualTo(created.id());
			assertThat(log.getPreviousStatus()).isEqualTo(RoomStatus.VACANT);
			assertThat(log.getNewStatus()).isEqualTo(RoomStatus.OCCUPIED);
			assertThat(log.getChangeReason()).isEqualTo("체크인");
			assertThat(log.getChangedBy()).isEqualTo(7L);
			assertThat(log.isConflict()).isFalse();
		});
	}

	@Test
	@DisplayName("화면이 본 상태와 실제 상태가 다르면 반려하고, 충돌(true) 이력을 남긴다")
	void changeStatusConflictIsRejectedAndLogged() {
		RoomResponse created = roomService.create(createRequest("101"));
		// 다른 담당자가 먼저 VACANT → OCCUPIED 로 바꿈
		roomService.changeStatus(created.id(), new RoomStatusChangeRequest(
				RoomStatus.VACANT, RoomStatus.OCCUPIED, "체크인", 7L));

		// 나는 아직 VACANT 로 보고 있는 화면에서 CLEANING 으로 바꾸려 함
		assertThatThrownBy(() -> roomService.changeStatus(created.id(), new RoomStatusChangeRequest(
				RoomStatus.VACANT, RoomStatus.CLEANING, "청소 시작", 8L)))
				.isInstanceOf(BusinessException.class)
				.hasMessage("다른 담당자가 먼저 객실 상태를 바꿨습니다. 새로고침 후 다시 시도해 주세요.");

		// 상태는 먼저 바꾼 값 그대로
		assertThat(roomService.getDetail(created.id()).status()).isEqualTo(RoomStatus.OCCUPIED);

		// 충돌 이력: 변경 전 상태는 DB 의 실제 상태(OCCUPIED)
		assertThat(roomStatusLogRepository.findAll())
				.filteredOn(RoomStatusLog::isConflict)
				.singleElement()
				.satisfies(log -> {
					assertThat(log.getPreviousStatus()).isEqualTo(RoomStatus.OCCUPIED);
					assertThat(log.getNewStatus()).isEqualTo(RoomStatus.CLEANING);
					assertThat(log.getChangedBy()).isEqualTo(8L);
				});
	}

	@Test
	@DisplayName("같은 상태로 바꾸면 BusinessException 이 나고 이력은 남지 않는다")
	void changeStatusToSameStatusThrows() {
		RoomResponse created = roomService.create(createRequest("101"));

		assertThatThrownBy(() -> roomService.changeStatus(created.id(), new RoomStatusChangeRequest(
				RoomStatus.VACANT, RoomStatus.VACANT, "확인", 7L)))
				.isInstanceOf(BusinessException.class)
				.hasMessage("이미 같은 상태입니다.");

		assertThat(roomStatusLogRepository.count()).isZero();
	}

	@Test
	@DisplayName("없는 객실의 상태를 바꾸면 BusinessException 이 난다")
	void changeStatusMissingRoomThrows() {
		assertThatThrownBy(() -> roomService.changeStatus(99999L, new RoomStatusChangeRequest(
				RoomStatus.VACANT, RoomStatus.OCCUPIED, "체크인", 7L)))
				.isInstanceOf(BusinessException.class)
				.hasMessage("객실을 찾을 수 없습니다.");
	}

	/** 테스트용 등록 요청. 타입·요금은 테스트마다 중요하지 않아 고정값으로 둡니다. */
	private RoomCreateRequest createRequest(String roomNumber) {
		return new RoomCreateRequest(roomNumber, "DOUBLE", new BigDecimal("100000.00"), null);
	}
}
