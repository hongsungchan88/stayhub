package com.stayhub.statistics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 객실 가동률 응답 (STA-001).
 *
 * <p>가동률 = 판매 객실·일 ÷ 판매 가능 객실·일 × 100 (소수 둘째 자리).
 * 하루짜리 구간이면 "그날 판매된 객실 수 ÷ 전체 객실 수" 와 같습니다.
 *
 * @param hasData 조회 기간에 투숙 예약이 하나도 없으면 false. 화면은 "데이터 없음" 을 표시합니다
 */
public record OccupancyResponse(
		long totalRooms,
		boolean hasData,
		List<Item> items
) {

	/**
	 * 구간 하나.
	 *
	 * @param soldRoomNights      구간 동안 판매된 객실 수의 합 (날짜마다 객실 중복 없이 센 값)
	 * @param availableRoomNights 전체 객실 수 × 구간 일수
	 */
	public record Item(
			LocalDate startDate,
			LocalDate endDate,
			long soldRoomNights,
			long availableRoomNights,
			BigDecimal occupancyRate
	) {
	}
}
