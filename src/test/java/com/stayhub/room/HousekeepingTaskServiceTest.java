package com.stayhub.room;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stayhub.common.BusinessException;
import com.stayhub.common.PageResponse;
import com.stayhub.room.dto.HousekeepingTaskCreateRequest;
import com.stayhub.room.dto.HousekeepingTaskResponse;
import com.stayhub.room.dto.HousekeepingTaskUpdateRequest;
import com.stayhub.room.entity.HousekeepingTaskStatus;
import com.stayhub.room.entity.HousekeepingTaskType;
import com.stayhub.room.repository.HousekeepingTaskRepository;
import com.stayhub.room.service.HousekeepingTaskService;
import java.time.LocalDateTime;
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

	@BeforeEach
	void setUp() {
		// 개발 DB 에 이미 들어 있는 데이터 때문에 결과가 흔들리지 않도록 테스트 안에서만 비웁니다.
		// @Transactional 이라 테스트가 끝나면 지운 것까지 원래대로 돌아옵니다.
		// 경고: @Transactional을 지우면 개발 DB의 이 테이블이 실제로 비워집니다. 지우지 마세요.
		housekeepingTaskRepository.deleteAll();
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

	/** 테스트용 등록 요청. 유형·스태프·재배정은 비워서 기본값을 쓰고, 시각은 고정값으로 둡니다. */
	private HousekeepingTaskCreateRequest createRequest(Long roomId) {
		return new HousekeepingTaskCreateRequest(roomId, null, CHECKOUT_TIME, null, null, SCHEDULED_AT);
	}
}
