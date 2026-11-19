package com.stayhub.statistics.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 채널별 예약 현황 응답 (STA-002).
 *
 * <p>예약 건수는 예약을 만든 날 기준(CANCELLED 제외), 매출은 결제일 기준입니다(총괄 결정 3·10).
 * 채널이 없는 예약(channel_id NULL)은 1번 "직접 예약" 으로 셉니다(총괄 결정 11).
 * 건수·매출이 0 인 채널도 비교 그래프에 나오도록 함께 돌려줍니다.
 */
public record ChannelStatisticsResponse(
		List<Item> items
) {

	public record Item(
			Long channelId,
			String channelName,
			long bookingCount,
			BigDecimal salesAmount
	) {
	}
}
