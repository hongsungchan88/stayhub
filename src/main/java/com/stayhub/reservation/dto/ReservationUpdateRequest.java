package com.stayhub.reservation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

/** 예약의 객실·체크인 날짜·체크아웃 날짜 변경 요청. */
public record ReservationUpdateRequest(
        @NotNull(message = "객실을 선택해 주세요.")
        @Positive(message = "객실 값이 올바르지 않습니다.")
        Long roomId,

        @NotNull(message = "체크인 날짜를 입력해 주세요.")
        LocalDate checkInDate,

        @NotNull(message = "체크아웃 날짜를 입력해 주세요.")
        LocalDate checkOutDate
) {
}