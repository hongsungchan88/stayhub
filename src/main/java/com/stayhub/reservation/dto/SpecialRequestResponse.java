package com.stayhub.reservation.dto;

import com.stayhub.reservation.entity.SpecialRequest;
import com.stayhub.reservation.entity.SpecialRequestStatus;
import java.time.LocalDateTime;

/** 특별 요청사항 조회·등록·변경 응답. */
public record SpecialRequestResponse(
        Long id,
        Long reservationId,
        String requestContent,
        SpecialRequestStatus status,
        String rejectReason,
        Long assignedStaffId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static SpecialRequestResponse from(SpecialRequest request) {
        return new SpecialRequestResponse(
                request.getId(),
                request.getReservationId(),
                request.getRequestContent(),
                request.getStatus(),
                request.getRejectReason(),
                request.getAssignedStaffId(),
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }
}