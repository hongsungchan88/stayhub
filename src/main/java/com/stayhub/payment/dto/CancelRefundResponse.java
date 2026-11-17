package com.stayhub.payment.dto;

import com.stayhub.payment.entity.PaymentRefundRule;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 예약 취소 환불 응답 (GET /api/payments/refund-preview, RSV-002 연동).
 *
 * <p>대상은 그 예약의 결제 완료(COMPLETED) 결제뿐입니다. 결제가 없으면 {@code refundRule = NONE}, 금액은 0 입니다.
 * 합산된 추가 요금은 환불액에 넣지 않습니다.
 */
public record CancelRefundResponse(
		Long reservationId,
		LocalDate checkInDate,
		PaymentRefundRule refundRule,
		BigDecimal paymentTotal,
		BigDecimal refundTotal,
		List<Item> payments
) {

	/** 결제 한 건의 결제 금액과 환불액. */
	public record Item(Long paymentId, BigDecimal paymentAmount, BigDecimal refundAmount) {
	}

	/** 항목으로 합계를 계산해 만듭니다. */
	public static CancelRefundResponse of(
			Long reservationId, LocalDate checkInDate, PaymentRefundRule refundRule, List<Item> payments) {
		return new CancelRefundResponse(
				reservationId,
				checkInDate,
				refundRule,
				payments.stream().map(Item::paymentAmount).reduce(BigDecimal.ZERO, BigDecimal::add),
				payments.stream().map(Item::refundAmount).reduce(BigDecimal.ZERO, BigDecimal::add),
				payments
		);
	}
}
