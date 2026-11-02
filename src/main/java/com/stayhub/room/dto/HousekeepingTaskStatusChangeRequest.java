package com.stayhub.room.dto;

import com.stayhub.room.entity.HousekeepingTaskStatus;
import jakarta.validation.constraints.NotNull;

/**
 * 청소/점검 작업 상태 변경 요청 (PUT /api/housekeeping-tasks/{id}/status 의 본문). ROM-003
 *
 * <p>{@code changedBy} 는 로그인 기능이 생기기 전까지 요청에서 받습니다.
 */
public record HousekeepingTaskStatusChangeRequest(

		@NotNull(message = "변경 후 상태를 선택해 주세요.")
		HousekeepingTaskStatus newStatus,

		@NotNull(message = "변경 담당자를 입력해 주세요.")
		Long changedBy
) {
}
