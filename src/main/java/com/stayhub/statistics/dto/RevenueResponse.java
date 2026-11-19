package com.stayhub.statistics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 매출 통계 응답 (STA-003).
 *
 * <p>매출 = 결제일 기준 net_revenue 합계(부분 환불은 비율만큼 뺌, 총괄 결정 3).
 * 전월 대비는 조회 기간을 한 달 앞으로 옮긴 기간과 비교합니다(총괄 결정 5).
 *
 * @param previousStartDate 비교 기간 시작일 (from 의 한 달 전)
 * @param previousEndDate   비교 기간 종료일 (to 의 한 달 전)
 * @param growthRate        (이번 − 비교) ÷ 비교 × 100, 소수 둘째 자리. 비교 기간 매출이 0 이면 null — 화면은 "비교 불가"
 */
public record RevenueResponse(
		BigDecimal totalRevenue,
		LocalDate previousStartDate,
		LocalDate previousEndDate,
		BigDecimal previousRevenue,
		BigDecimal growthRate,
		List<Item> items
) {

	public record Item(
			LocalDate startDate,
			LocalDate endDate,
			BigDecimal revenue
	) {
	}
}
