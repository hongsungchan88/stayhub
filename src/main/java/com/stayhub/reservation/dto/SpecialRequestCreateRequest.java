package com.stayhub.reservation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** 특별 요청사항 등록 요청. */
public record SpecialRequestCreateRequest(
        @NotNull(message = "예약을 선택해 주세요.")
        @Positive(message = "예약 값이 올바르지 않습니다.")
        Long reservationId,

        @NotBlank(message = "요청 내용을 입력해 주세요.")
        String requestContent
) {
}