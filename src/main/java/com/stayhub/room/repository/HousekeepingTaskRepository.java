package com.stayhub.room.repository;

import com.stayhub.room.entity.HousekeepingTask;
import com.stayhub.room.entity.HousekeepingTaskStatus;
import com.stayhub.room.entity.HousekeepingTaskType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 청소/점검 작업 스케줄 DB 접근.
 *
 * <p>유형 ②(업무 상태 있음) 테이블이고 목록 필터가 없어서, 목록·상세·저장은 {@code JpaRepository} 기본 메서드
 * {@code findAll(pageable)}·{@code findById}·{@code save}로 충분합니다.
 * 따로 선언한 메서드는 체크아웃 후 처리에서 쓰는 아래 하나뿐입니다.
 */
public interface HousekeepingTaskRepository extends JpaRepository<HousekeepingTask, Long> {

	/**
	 * 그 객실의 작업 중 유형·상태가 맞는 것 하나를 배정 일시가 이른 순서로 찾습니다.
	 * 체크아웃 때 미리 만들어 둔 청소 작업(CLEANING, SCHEDULED)이 있는지 볼 때 씁니다.
	 */
	Optional<HousekeepingTask> findFirstByRoomIdAndTaskTypeAndStatusOrderByScheduledAtAscIdAsc(
			Long roomId, HousekeepingTaskType taskType, HousekeepingTaskStatus status);
}
