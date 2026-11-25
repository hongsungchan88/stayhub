package com.stayhub.reservation.dto;

import com.stayhub.reservation.entity.ReservationStatus;
import java.time.LocalDate;

/** 본인 확인을 마친 게스트에게 표시하는 예약 한 건입니다. */
public record GuestReservationLookupResponse(
        String reservationCode,
        String roomNumber,
        LocalDate checkIn,
        LocalDate checkOut,
        int guestCount,
        ReservationStatus status
) {
}
