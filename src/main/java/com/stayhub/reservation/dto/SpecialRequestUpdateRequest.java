package com.stayhub.reservation.dto;

import com.stayhub.reservation.entity.SpecialRequestStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** 특별 요청사항 처리 정보 변경 요청. */
public record SpecialRequestUpdateRequest(
        @NotNull(message = "처리 상태를 선택해 주세요.")
        SpecialRequestStatus status,

        @Size(max = 255, message = "거절 사유는 255자 이내로 입력해 주세요.")
        String rejectReason,

        @Positive(message = "담당자 값이 올바르지 않습니다.")
        Long assignedStaffId
) {
}