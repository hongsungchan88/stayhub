package com.stayhub.room.repository;

import com.stayhub.room.entity.HousekeepingTask;
import com.stayhub.room.entity.HousekeepingTaskStatus;
import com.stayhub.room.entity.HousekeepingTaskType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 청소/점검 작업 스케줄 DB 접근.
 *
 * <p>유형 ②(업무 상태 있음) 테이블이고 목록 필터가 없어서, 목록·상세·저장은 {@code JpaRepository} 기본 메서드
 * {@code findAll(pageable)}·{@code findById}·{@code save}로 충분합니다.
 * 따로 선언한 메서드는 체크아웃 후 처리와 일정 중복 알림에서 쓰는 아래 두 개입니다.
 */
public interface HousekeepingTaskRepository extends JpaRepository<HousekeepingTask, Long> {

	/**
	 * 그 객실의 작업 중 유형·상태가 맞는 것 하나를 배정 일시가 이른 순서로 찾습니다.
	 * 체크아웃 때 미리 만들어 둔 청소 작업(CLEANING, SCHEDULED)이 있는지 볼 때 씁니다.
	 */
	Optional<HousekeepingTask> findFirstByRoomIdAndTaskTypeAndStatusOrderByScheduledAtAscIdAsc(
			Long roomId, HousekeepingTaskType taskType, HousekeepingTaskStatus status);

	/**
	 * 그 스태프에게 같은 배정 일시로 잡힌 작업 중 상태가 {@code status}가 아닌 것을 모두 찾습니다.
	 * 일정 중복 관리자 알림(ROM-003)에서 끝나지 않은 작업이 겹치는지 볼 때 씁니다.
	 */
	List<HousekeepingTask> findByAssignedStaffIdAndScheduledAtAndStatusNot(
			Long assignedStaffId, LocalDateTime scheduledAt, HousekeepingTaskStatus status);
}
