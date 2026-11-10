package com.stayhub.room;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stayhub.common.BusinessException;
import com.stayhub.common.PageResponse;
import com.stayhub.room.dto.HousekeepingTaskCreateRequest;
import com.stayhub.room.dto.HousekeepingTaskResponse;
import com.stayhub.room.dto.HousekeepingTaskStatusChangeRequest;
import com.stayhub.room.dto.HousekeepingTaskUpdateRequest;
import com.stayhub.room.dto.RoomCreateRequest;
import com.stayhub.room.dto.RoomStatusChangeRequest;
import com.stayhub.room.entity.HousekeepingTaskStatus;
import com.stayhub.room.entity.HousekeepingTaskType;
import com.stayhub.room.entity.RoomStatus;
import com.stayhub.room.entity.RoomStatusLog;
import com.stayhub.room.repository.HousekeepingTaskRepository;
import com.stayhub.room.repository.RoomRepository;
import com.stayhub.room.repository.RoomStatusLogRepository;
import com.stayhub.room.service.HousekeepingTaskService;
import com.stayhub.room.service.RoomService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 청소/점검 작업 스케줄 서비스 테스트.
 *
 * <p>실제 DB(application-local.yml 의 MySQL)에 붙어서 등록·조회·수정이 제대로 되는지 확인합니다.
 * {@code @Transactional} 이 붙어 있어서 테스트가 끝나면 넣었던 데이터가 전부 되돌려집니다(롤백).
 * 유형 ②(업무 상태 있음) 테이블이라 삭제 테스트는 없습니다.
 */
@SpringBootTest
@Transactional
class HousekeepingTaskServiceTest {

	private static final LocalDateTime CHECKOUT_TIME = LocalDateTime.of(2026, 10, 12, 11, 0);
	private static final LocalDateTime SCHEDULED_AT = LocalDateTime.of(2026, 10, 12, 12, 0);

	@Autowired
	private HousekeepingTaskService housekeepingTaskService;

	@Autowired
	private HousekeepingTaskRepository housekeepingTaskRepository;

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
		housekeepingTaskRepository.deleteAll();
		roomStatusLogRepository.deleteAll();
		roomRepository.deleteAll();
	}

	@Test
	@DisplayName("등록하면 목록에 나온다")
	void createThenAppearsInList() {
		HousekeepingTaskResponse created = housekeepingTaskService.create(createRequest(1L));

		PageResponse<HousekeepingTaskResponse> all = housekeepingTaskService.getList(0);
		assertThat(all.content()).extracting(HousekeepingTaskResponse::id).containsExactly(created.id());

		// 비워 보낸 값은 정의서의 기본값으로 시작한다
		assertThat(created.taskType()).isEqualTo(HousekeepingTaskType.CLEANING);
		assertThat(created.status()).isEqualTo(HousekeepingTaskStatus.SCHEDULED);
		assertThat(created.isReassigned()).isFalse();
		assertThat(created.completedAt()).isNull();
	}

	@Test
	@DisplayName("수정하면 바뀐 값이 조회된다")
	void updateIsApplied() {
		HousekeepingTaskResponse created = housekeepingTaskService.create(createRequest(1L));

		LocalDateTime newScheduledAt = SCHEDULED_AT.plusHours(2);
		housekeepingTaskService.update(created.id(), new HousekeepingTaskUpdateRequest(
				2L, HousekeepingTaskType.INSPECTION, CHECKOUT_TIME, 5L, true, newScheduledAt));

		HousekeepingTaskResponse found = housekeepingTaskService.getDetail(created.id());
		assertThat(found.roomId()).isEqualTo(2L);
		assertThat(found.taskType()).isEqualTo(HousekeepingTaskType.INSPECTION);
		assertThat(found.assignedStaffId()).isEqualTo(5L);
		assertThat(found.isReassigned()).isTrue();
		assertThat(found.scheduledAt()).isEqualTo(newScheduledAt);
	}

	@Test
	@DisplayName("없는 항목을 상세 조회하면 BusinessException 이 난다")
	void getMissingThrows() {
		assertThatThrownBy(() -> housekeepingTaskService.getDetail(99999L))
				.isInstanceOf(BusinessException.class)
				.hasMessage("청소/점검 작업을 찾을 수 없습니다.");
	}

	@Test
	@DisplayName("청소 시작하면 작업은 진행 중, 객실은 청소중이 되고 이력이 남는다")
	void startCleaningChangesRoom() {
		Long roomId = roomNeedingCleaning();
		HousekeepingTaskResponse task = housekeepingTaskService.create(createRequest(roomId));

		HousekeepingTaskResponse started = changeTaskStatus(task.id(), HousekeepingTaskStatus.IN_PROGRESS);

		assertThat(started.status()).isEqualTo(HousekeepingTaskStatus.IN_PROGRESS);
		assertThat(started.completedAt()).isNull();
		assertThat(roomService.getDetail(roomId).status()).isEqualTo(RoomStatus.CLEANING);
		assertThat(lastLog().getChangeReason()).isEqualTo("청소 시작");
		assertThat(lastLog().getChangedBy()).isEqualTo(7L);
	}

	@Test
	@DisplayName("청소 완료하면 완료 일시가 남고 객실은 빈방이 된다")
	void completeCleaningFreesRoom() {
		Long roomId = roomNeedingCleaning();
		HousekeepingTaskResponse task = housekeepingTaskService.create(createRequest(roomId));
		changeTaskStatus(task.id(), HousekeepingTaskStatus.IN_PROGRESS);

		HousekeepingTaskResponse completed = changeTaskStatus(task.id(), HousekeepingTaskStatus.COMPLETED);

		assertThat(completed.status()).isEqualTo(HousekeepingTaskStatus.COMPLETED);
		assertThat(completed.completedAt()).isNotNull();
		assertThat(roomService.getDetail(roomId).status()).isEqualTo(RoomStatus.VACANT);
		assertThat(lastLog().getChangeReason()).isEqualTo("청소 완료");
	}

	@Test
	@DisplayName("점검 작업은 완료해도 객실 상태를 바꾸지 않는다")
	void inspectionDoesNotChangeRoom() {
		Long roomId = createRoom();
		HousekeepingTaskResponse task = housekeepingTaskService.create(new HousekeepingTaskCreateRequest(
				roomId, HousekeepingTaskType.INSPECTION, CHECKOUT_TIME, null, null, SCHEDULED_AT));

		changeTaskStatus(task.id(), HousekeepingTaskStatus.IN_PROGRESS);
		HousekeepingTaskResponse completed = changeTaskStatus(task.id(), HousekeepingTaskStatus.COMPLETED);

		assertThat(completed.completedAt()).isNotNull();
		assertThat(roomService.getDetail(roomId).status()).isEqualTo(RoomStatus.VACANT);
		assertThat(roomStatusLogRepository.findAll()).isEmpty();
	}

	@Test
	@DisplayName("순서를 건너뛰거나 되돌리거나 같은 상태로 바꾸면 BusinessException 이 난다")
	void invalidTransitionsThrow() {
		Long roomId = roomNeedingCleaning();
		HousekeepingTaskResponse task = housekeepingTaskService.create(createRequest(roomId));

		assertInvalidTransition(task.id(), HousekeepingTaskStatus.COMPLETED); // 건너뛰기
		assertInvalidTransition(task.id(), HousekeepingTaskStatus.SCHEDULED); // 같은 상태
		assertThat(roomService.getDetail(roomId).status()).isEqualTo(RoomStatus.CLEANING_NEEDED);

		changeTaskStatus(task.id(), HousekeepingTaskStatus.IN_PROGRESS);
		assertInvalidTransition(task.id(), HousekeepingTaskStatus.SCHEDULED); // 되돌리기

		changeTaskStatus(task.id(), HousekeepingTaskStatus.COMPLETED);
		assertInvalidTransition(task.id(), HousekeepingTaskStatus.IN_PROGRESS); // 완료 후 변경
	}

	@Test
	@DisplayName("객실이 청소필요 상태가 아니면 청소를 시작할 수 없고 작업도 그대로다")
	void roomInWrongStatusThrows() {
		Long roomId = createRoom(); // VACANT
		HousekeepingTaskResponse task = housekeepingTaskService.create(createRequest(roomId));

		assertThatThrownBy(() -> changeTaskStatus(task.id(), HousekeepingTaskStatus.IN_PROGRESS))
				.isInstanceOf(BusinessException.class)
				.hasMessage("객실이 청소 필요 상태가 아니라 청소를 시작할 수 없습니다.");

		assertThat(housekeepingTaskService.getDetail(task.id()).status()).isEqualTo(HousekeepingTaskStatus.SCHEDULED);
		assertThat(roomService.getDetail(roomId).status()).isEqualTo(RoomStatus.VACANT);
	}

	@Test
	@DisplayName("객실이 청소중 상태가 아니면 청소를 완료할 수 없고 작업도 그대로다")
	void roomNotCleaningCannotComplete() {
		Long roomId = roomNeedingCleaning();
		HousekeepingTaskResponse task = housekeepingTaskService.create(createRequest(roomId));
		changeTaskStatus(task.id(), HousekeepingTaskStatus.IN_PROGRESS);
		// 그 사이 다른 담당자가 객실을 투숙중으로 바꿈
		roomService.changeStatus(roomId, new RoomStatusChangeRequest(
				RoomStatus.CLEANING, RoomStatus.OCCUPIED, "체크인"), 1L);

		assertThatThrownBy(() -> changeTaskStatus(task.id(), HousekeepingTaskStatus.COMPLETED))
				.isInstanceOf(BusinessException.class)
				.hasMessage("객실이 청소 중 상태가 아니라 청소를 완료할 수 없습니다.");

		assertThat(housekeepingTaskService.getDetail(task.id()).status()).isEqualTo(HousekeepingTaskStatus.IN_PROGRESS);
		assertThat(roomService.getDetail(roomId).status()).isEqualTo(RoomStatus.OCCUPIED);
	}

	@Test
	@DisplayName("없는 작업의 상태를 바꾸면 BusinessException 이 난다")
	void changeStatusOfMissingThrows() {
		assertThatThrownBy(() -> changeTaskStatus(99999L, HousekeepingTaskStatus.IN_PROGRESS))
				.isInstanceOf(BusinessException.class)
				.hasMessage("청소/점검 작업을 찾을 수 없습니다.");
	}

	private HousekeepingTaskResponse changeTaskStatus(Long taskId, HousekeepingTaskStatus newStatus) {
		return housekeepingTaskService.changeStatus(taskId, new HousekeepingTaskStatusChangeRequest(newStatus), 7L);
	}

	private void assertInvalidTransition(Long taskId, HousekeepingTaskStatus newStatus) {
		assertThatThrownBy(() -> changeTaskStatus(taskId, newStatus))
				.isInstanceOf(BusinessException.class)
				.hasMessage("작업 상태는 배정됨 → 진행 중 → 완료 순서로만 바꿀 수 있습니다.");
	}

	/** 빈방(VACANT) 객실을 만들고 ID 를 돌려줍니다. */
	private Long createRoom() {
		return roomService.create(new RoomCreateRequest("HK-101", "싱글", new BigDecimal("50000.00"), 1)).id();
	}

	/** 체크아웃이 끝나 청소필요(CLEANING_NEEDED) 상태인 객실을 만들고 ID 를 돌려줍니다. */
	private Long roomNeedingCleaning() {
		Long roomId = createRoom();
		roomService.changeStatus(roomId, new RoomStatusChangeRequest(
				RoomStatus.VACANT, RoomStatus.CLEANING_NEEDED, "체크아웃"), 1L);
		return roomId;
	}

	/** 가장 최근 객실 상태 변경 이력. */
	private RoomStatusLog lastLog() {
		List<RoomStatusLog> logs = roomStatusLogRepository.findAll();
		return logs.get(logs.size() - 1);
	}

	/** 테스트용 등록 요청. 유형·스태프·재배정은 비워서 기본값을 쓰고, 시각은 고정값으로 둡니다. */
	private HousekeepingTaskCreateRequest createRequest(Long roomId) {
		return new HousekeepingTaskCreateRequest(roomId, null, CHECKOUT_TIME, null, null, SCHEDULED_AT);
	}
}
