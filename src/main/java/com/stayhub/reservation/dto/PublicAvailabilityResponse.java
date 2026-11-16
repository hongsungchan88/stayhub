package com.stayhub.reservation.dto;

import com.stayhub.room.entity.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 게스트 빈 객실 조회 결과 (CMS-008). 객실 상태·version 같은 내부 값은 담지 않습니다.
 *
 * @param rooms            빈 객실. 비어 있으면 alternativeDates 를 봅니다
 * @param alternativeDates 빈 객실이 없을 때만 채워지는 대체 날짜(같은 박수, 최대 3개)
 */
public record PublicAvailabilityResponse(
		List<AvailableRoom> rooms,
		List<AlternativeDate> alternativeDates
) {

	public record AvailableRoom(
			Long roomId,
			String roomNumber,
			String roomType,
			int maxOccupancy,
			BigDecimal pricePerNight,
			BigDecimal totalPrice
	) {
		public static AvailableRoom of(Room room, long nights) {
			return new AvailableRoom(
					room.getId(),
					room.getRoomNumber(),
					room.getRoomType(),
					room.getMaxOccupancy(),
					room.getBasePrice(),
					room.getBasePrice().multiply(BigDecimal.valueOf(nights))
			);
		}
	}

	public record AlternativeDate(LocalDate checkIn, LocalDate checkOut) {
	}
}
