package com.stayhub.room.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 객실 등록 요청 (POST /api/rooms 의 본문).
 *
 * <p>여기 없는 컬럼 — {@code room_id}, {@code property_id}, {@code status}, {@code status_updated_by},
 * {@code status_updated_at}, {@code created_at}, {@code updated_at} — 은 요청에서 받지 않습니다. 서버가 정하는 값입니다.
 */
public record RoomCreateRequest(

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
