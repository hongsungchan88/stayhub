package com.stayhub.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 결제 등록 요청 (POST /api/payments 의 본문).
 *
 * <p>여기 없는 컬럼 — {@code payment_id}, {@code payment_status}, {@code refund_request_date}, {@code refund_amount},
 * {@code refund_rule_applied}, {@code created_at}, {@code updated_at} — 은 요청에서 받지 않습니다.
 * 결제 상태는 COMPLETED 로 시작하고, 환불 값은 환불 처리 기능에서 채웁니다.
 */
public record PaymentCreateRequest(

		@NotNull(message = "예약을 선택해 주세요.")
		Long reservationId,

		@NotNull(message = "예약 채널을 선택해 주세요.")
		Long channelId,

		@NotBlank(message = "결제 수단을 입력해 주세요.")
		@Size(max = 20, message = "결제 수단은 20자 이내로 입력해 주세요.")
		String paymentMethod,

		@NotNull(message = "결제 금액을 입력해 주세요.")
		BigDecimal paymentAmount,

		// 선택 입력. 비우면 0.
		BigDecimal commissionAmount,

		// 선택 입력. 비우면 0.
		BigDecimal netRevenue,

		// 선택 입력.
		@Size(max = 255, message = "오류 내역은 255자 이내로 입력해 주세요.")
		String errorNote
) {
}
