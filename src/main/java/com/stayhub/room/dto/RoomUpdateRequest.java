package com.stayhub.room.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 객실 수정 요청 (PUT /api/rooms/{id} 의 본문).
 *
 * <p>등록 요청과 같은 모양입니다. PUT 은 "통째로 바꾸기" 라서 모든 값을 다시 보냅니다.
 * maxOccupancy 를 비워 보내면 1 로 돌아갑니다.
 * 객실 상태(status)는 이 요청으로 바꾸지 않습니다.
 */
public record RoomUpdateRequest(

		@NotBlank(message = "객실 번호를 입력해 주세요.")
		@Size(max = 20, message = "객실 번호는 20자 이내로 입력해 주세요.")
		String roomNumber,

		@NotBlank(message = "객실 타입을 입력해 주세요.")
		@Size(max = 20, message = "객실 타입은 20자 이내로 입력해 주세요.")
		String roomType,

		@NotNull(message = "기본 요금을 입력해 주세요.")
		BigDecimal basePrice,

		// 선택 입력. 비우면 1.
		Integer maxOccupancy
) {
}
