package com.stayhub.room.dto;

import com.stayhub.room.entity.HousekeepingTask;
import com.stayhub.room.entity.HousekeepingTaskStatus;
import com.stayhub.room.entity.HousekeepingTaskType;
import java.time.LocalDateTime;

/**
 * 청소/점검 작업 응답 (목록·상세·등록·수정이 모두 이 모양으로 나갑니다).
 *
 * <p>작업 상태(status)는 업무 상태라서, roomId·assignedStaffId 는 화면에 필요한 참조 ID 라서 넣습니다.
 * 변환({@link #from})은 서비스 안에서 호출합니다. 컨트롤러는 엔티티를 만지지 않습니다.
 */
public record HousekeepingTaskResponse(
		Long id,
		Long roomId,
		HousekeepingTaskType taskType,
		LocalDateTime checkoutTime,
		Long assignedStaffId,
		HousekeepingTaskStatus status,
		boolean isReassigned,
		LocalDateTime scheduledAt,
		LocalDateTime completedAt,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {

	/** 엔티티를 응답 모양으로 바꿉니다. */
	public static HousekeepingTaskResponse from(HousekeepingTask housekeepingTask) {
		return new HousekeepingTaskResponse(
				housekeepingTask.getId(),
				housekeepingTask.getRoomId(),
				housekeepingTask.getTaskType(),
				housekeepingTask.getCheckoutTime(),
				housekeepingTask.getAssignedStaffId(),
				housekeepingTask.getStatus(),
				housekeepingTask.isReassigned(),
				housekeepingTask.getScheduledAt(),
				housekeepingTask.getCompletedAt(),
				housekeepingTask.getCreatedAt(),
				housekeepingTask.getUpdatedAt()
		);
	}
}
