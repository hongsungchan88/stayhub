package com.stayhub.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 결제 수정 요청 (PUT /api/payments/{id} 의 본문).
 *
 * <p>등록 요청과 같은 모양입니다. PUT 은 "통째로 바꾸기" 라서 모든 값을 다시 보냅니다.
 * commissionAmount·netRevenue 를 비워 보내면 0 으로 돌아갑니다.
 * 결제 상태(paymentStatus)와 환불 값은 이 요청으로 바꾸지 않습니다.
 */
public record PaymentUpdateRequest(

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
