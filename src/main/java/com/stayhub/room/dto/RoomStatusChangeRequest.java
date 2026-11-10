package com.stayhub.room.dto;

import com.stayhub.room.entity.RoomStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 객실 상태 변경 요청 (PUT /api/rooms/{id}/status 의 본문). ROM-001
 *
 * <p>{@code previousStatus} 는 화면이 보고 있던 현재 상태입니다. DB 의 실제 상태와 다르면
 * 그 사이에 다른 담당자가 먼저 바꾼 것으로 보고 반려합니다(동시 변경 충돌).
 * 변경 담당자는 요청에서 받지 않고 로그인 세션에서 가져옵니다.
 */
public record RoomStatusChangeRequest(

		@NotNull(message = "변경 전 상태를 선택해 주세요.")
		RoomStatus previousStatus,

		@NotNull(message = "변경 후 상태를 선택해 주세요.")
		RoomStatus newStatus,

		@NotBlank(message = "변경 사유를 입력해 주세요.")
		@Size(max = 30, message = "변경 사유는 30자 이내로 입력해 주세요.")
		String changeReason
) {
}
