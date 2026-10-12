package com.stayhub.room.dto;

import com.stayhub.room.entity.RoomStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 객실 상태 변경 이력 수정 요청 (PUT /api/room-status-logs/{id} 의 본문).
 *
 * <p>등록 요청과 같은 모양입니다. PUT 은 "통째로 바꾸기" 라서 모든 값을 다시 보냅니다.
 * isConflict 를 비워 보내면 false 로 돌아갑니다.
 */
public record RoomStatusLogUpdateRequest(

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
