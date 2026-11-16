package com.stayhub.reservation.dto;

import com.stayhub.reservation.entity.Reservation;
import com.stayhub.room.entity.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 게스트 예약 완료 응답. 예약번호·객실 번호·날짜·인원·총 가격만 — 예약 ID·상태·연락처 등 내부 값은 담지 않습니다.
 */
public record PublicReservationResponse(
		String reservationCode,
		String roomNumber,
		LocalDate checkIn,
		LocalDate checkOut,
		int guestCount,
		BigDecimal totalPrice
) {
	public static PublicReservationResponse of(Reservation reservation, Room room) {
		long nights = ChronoUnit.DAYS.between(reservation.getCheckInDate(), reservation.getCheckOutDate());
		return new PublicReservationResponse(
				reservation.getReservationCode(),
				room.getRoomNumber(),
				reservation.getCheckInDate(),
				reservation.getCheckOutDate(),
				reservation.getGuestCount(),
				room.getBasePrice().multiply(BigDecimal.valueOf(nights))
		);
	}
}
