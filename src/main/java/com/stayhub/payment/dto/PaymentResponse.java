package com.stayhub.payment.dto;

import com.stayhub.payment.entity.Payment;
import com.stayhub.payment.entity.PaymentRefundRule;
import com.stayhub.payment.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 결제 응답 (목록·상세·등록·수정이 모두 이 모양으로 나갑니다).
 *
 * <p>결제 상태(paymentStatus)는 업무 상태라서, reservationId·channelId 는 화면에 필요한 참조 ID 라서 넣습니다.
 * 변환({@link #from})은 서비스 안에서 호출합니다. 컨트롤러는 엔티티를 만지지 않습니다.
 */
public record PaymentResponse(
		Long id,
		Long reservationId,
		Long channelId,
		String paymentMethod,
		BigDecimal paymentAmount,
		BigDecimal commissionAmount,
		BigDecimal netRevenue,
		PaymentStatus paymentStatus,
		LocalDate refundRequestDate,
		BigDecimal refundAmount,
		PaymentRefundRule refundRuleApplied,
		String errorNote,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {

	/** 엔티티를 응답 모양으로 바꿉니다. */
	public static PaymentResponse from(Payment payment) {
		return new PaymentResponse(
				payment.getId(),
				payment.getReservationId(),
				payment.getChannelId(),
				payment.getPaymentMethod(),
				payment.getPaymentAmount(),
				payment.getCommissionAmount(),
				payment.getNetRevenue(),
				payment.getPaymentStatus(),
				payment.getRefundRequestDate(),
				payment.getRefundAmount(),
				payment.getRefundRuleApplied(),
				payment.getErrorNote(),
				payment.getCreatedAt(),
				payment.getUpdatedAt()
		);
	}
}
