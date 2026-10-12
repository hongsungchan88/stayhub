package com.stayhub.room.dto;

import com.stayhub.room.entity.HousekeepingTaskType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 청소/점검 작업 등록 요청 (POST /api/housekeeping-tasks 의 본문).
 *
 * <p>여기 없는 컬럼 — {@code task_id}, {@code status}, {@code completed_at}, {@code created_at},
 * {@code updated_at} — 은 요청에서 받지 않습니다. 서버가 정하는 값입니다.
 */
public record HousekeepingTaskCreateRequest(

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
