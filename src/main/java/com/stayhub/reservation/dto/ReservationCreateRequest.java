package com.stayhub.reservation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 예약 등록 요청.
 * 화면에서 입력한 값을 받으며, 예약번호와 초기 상태는 서버에서 정합니다.
 */
public record ReservationCreateRequest(
        @NotBlank(message = "예약자 이름을 입력해 주세요.")
        @Size(max = 50, message = "예약자 이름은 50자 이내로 입력해 주세요.")
        String guestName,

        @NotBlank(message = "연락처를 입력해 주세요.")
        @Size(max = 20, message = "연락처는 20자 이내로 입력해 주세요.")
        String guestPhone,

        @NotNull(message = "객실을 선택해 주세요.")
        @Positive(message = "객실 값이 올바르지 않습니다.")
        Long roomId,

        @Positive(message = "예약 채널 값이 올바르지 않습니다.")
        Long channelId,

        @NotNull(message = "체크인 날짜를 입력해 주세요.")
        LocalDate checkInDate,

        @NotNull(message = "체크아웃 날짜를 입력해 주세요.")
        LocalDate checkOutDate,

        @NotNull(message = "투숙 인원을 입력해 주세요.")
        @Min(value = 1, message = "투숙 인원은 1명 이상이어야 합니다.")
        Integer guestCount
) {
}