package com.stayhub.reservation.dto;

import jakarta.validation.constraints.Size;

/** 예약 취소 요청. 취소 사유는 선택 입력입니다. */
public record ReservationCancelRequest(
        @Size(max = 255, message = "취소 사유는 255자 이내로 입력해 주세요.")
        String cancelReason
) {
}