package com.stayhub.room.dto;

import com.stayhub.room.entity.RoomStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 객실 상태 변경 이력 등록 요청 (POST /api/room-status-logs 의 본문).
 *
 * <p>여기 없는 컬럼 — {@code status_log_id}, {@code created_at}, {@code updated_at} — 은 요청에서 받지 않습니다. 서버가 정하는 값입니다.
 */
public record RoomStatusLogCreateRequest(

		@NotNull(message = "객실을 선택해 주세요.")
		Long roomId,

		@NotNull(message = "변경 전 상태를 선택해 주세요.")
		RoomStatus previousStatus,

		@NotNull(message = "변경 후 상태를 선택해 주세요.")
		RoomStatus newStatus,

		@NotBlank(message = "변경 사유를 입력해 주세요.")
		@Size(max = 30, message = "변경 사유는 30자 이내로 입력해 주세요.")
		String changeReason,

		@NotNull(message = "변경 담당자를 입력해 주세요.")
		Long changedBy,

		// 선택 입력. 비우면 false.
		Boolean isConflict
) {
}
