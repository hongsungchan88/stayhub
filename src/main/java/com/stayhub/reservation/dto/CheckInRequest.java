package com.stayhub.reservation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CheckInRequest(
        @NotBlank(message = "키 번호를 입력해 주세요.")
        @Size(max = 50, message = "키 번호는 50자 이내로 입력해 주세요.")
        String keyCode
) {}