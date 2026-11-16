package com.stayhub.reservation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 게스트 예약 요청 (SCR-GST-03). 길이는 테이블 정의서 reservations.guest_name VARCHAR(50)·guest_phone VARCHAR(20).
 * 채널·회원은 받지 않습니다(서버가 NULL 로 저장).
 */
public record PublicReservationRequest(
		@NotNull(message = "객실을 선택해 주세요.")
		@Positive(message = "객실 값이 올바르지 않습니다.")
		Long roomId,

		@NotNull(message = "체크인 날짜를 입력해 주세요.")
		LocalDate checkIn,

		@NotNull(message = "체크아웃 날짜를 입력해 주세요.")
		LocalDate checkOut,

		@NotNull(message = "투숙 인원을 입력해 주세요.")
		@Min(value = 1, message = "투숙 인원은 1명 이상이어야 합니다.")
		Integer guestCount,

		@NotBlank(message = "예약자 이름을 입력해 주세요.")
		@Size(max = 50, message = "예약자 이름은 50자 이내로 입력해 주세요.")
		String guestName,

		@NotBlank(message = "연락처를 입력해 주세요.")
		@Size(max = 20, message = "연락처는 20자 이내로 입력해 주세요.")
		String guestPhone
) {
}
