package com.stayhub.reservation.dto;

import com.stayhub.reservation.entity.Reservation;
import com.stayhub.reservation.entity.ReservationStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 예약 응답.
 * 객실·채널의 ID와 함께 화면에 표시할 번호·이름을 반환합니다.
 */
public record ReservationResponse(
        Long id,
        String reservationCode,
        String guestName,
        String guestPhone,
        Long roomId,
        String roomNumber,
        Long channelId,
        String channelName,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        int guestCount,
        ReservationStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ReservationResponse from(
            Reservation reservation,
            String roomNumber,
            String channelName
    ) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getReservationCode(),
                reservation.getGuestName(),
                reservation.getGuestPhone(),
                reservation.getRoomId(),
                roomNumber,
                reservation.getChannelId(),
                channelName,
                reservation.getCheckInDate(),
                reservation.getCheckOutDate(),
                reservation.getGuestCount(),
                reservation.getStatus(),
                reservation.getCreatedAt(),
                reservation.getUpdatedAt()
        );
    }
}