package com.stayhub.room.dto;

import com.stayhub.room.entity.HousekeepingTaskStatus;
import jakarta.validation.constraints.NotNull;

/**
 * 청소/점검 작업 상태 변경 요청 (PUT /api/housekeeping-tasks/{id}/status 의 본문). ROM-003
 *
 * <p>변경 담당자는 요청에서 받지 않고 로그인 세션에서 가져옵니다.
 */
public record HousekeepingTaskStatusChangeRequest(

		@NotNull(message = "변경 후 상태를 선택해 주세요.")
		HousekeepingTaskStatus newStatus
) {
}
