package com.stayhub.room.dto;

import com.stayhub.room.entity.HousekeepingTaskType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 청소/점검 작업 수정 요청 (PUT /api/housekeeping-tasks/{id} 의 본문).
 *
 * <p>등록 요청과 같은 모양입니다. PUT 은 "통째로 바꾸기" 라서 모든 값을 다시 보냅니다.
 * taskType 을 비워 보내면 CLEANING, isReassigned 를 비워 보내면 false 로 돌아갑니다.
 * 작업 상태(status)·완료 일시(completedAt)는 이 요청으로 바꾸지 않습니다.
 */
public record HousekeepingTaskUpdateRequest(

		@NotNull(message = "객실을 선택해 주세요.")
		Long roomId,

		// 선택 입력. 비우면 CLEANING.
		HousekeepingTaskType taskType,

		@NotNull(message = "체크아웃 완료 시각을 입력해 주세요.")
		LocalDateTime checkoutTime,

		// 선택 입력. 아직 배정 전이면 비웁니다.
		Long assignedStaffId,

		// 선택 입력. 비우면 false.
		Boolean isReassigned,

		@NotNull(message = "작업 배정 일시를 입력해 주세요.")
		LocalDateTime scheduledAt
) {
}
