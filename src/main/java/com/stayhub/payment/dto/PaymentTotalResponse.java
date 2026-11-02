package com.stayhub.payment.dto;

import java.math.BigDecimal;

/**
 * 결제 합계 응답 (GET /api/payments/{id}/total). PAY-002
 *
 * <p>{@code totalAmount} 는 결제 금액에 이 결제로 합산된 추가 요금을 더한 값입니다.
 * 별도 청구({@code isMerged = false}) 추가 요금은 포함하지 않습니다.
 */
public record PaymentTotalResponse(
		Long paymentId,
		BigDecimal paymentAmount,
		BigDecimal mergedChargeAmount,
		BigDecimal totalAmount
) {
}
