package com.stayhub.reservation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 비회원 예약 조회 시 본인 확인에 사용하는 입력입니다. */
public record GuestReservationLookupRequest(
        @NotBlank(message = "예약번호를 입력해 주세요.")
        @Size(max = 50, message = "예약번호는 50자 이하여야 합니다.")
        String reservationCode,

        @NotBlank(message = "연락처를 입력해 주세요.")
        @Size(max = 20, message = "연락처는 20자 이하여야 합니다.")
        String guestPhone
) {
}
