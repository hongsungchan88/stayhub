package com.stayhub.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 환불 거절 요청 (PUT /api/payments/{id}/refund-rejection 의 본문). PAY-001
 *
 * <p>거절 사유는 결제의 {@code error_note} 에 저장됩니다. 정의서의 "규정 외 환불 요청 시 오류 처리" 를 사람이 판단해 기록하는 자리입니다.
 */
public record PaymentRefundRejectRequest(

		@NotBlank(message = "거절 사유를 입력해 주세요.")
		@Size(max = 255, message = "거절 사유는 255자 이내로 입력해 주세요.")
		String reason
) {
}
