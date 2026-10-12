package com.stayhub.payment.entity;

/**
 * 결제 상태.
 *
 * <p>테이블 정의서의 {@code payment_status VARCHAR(20) — COMPLETED, FAILED, REFUND_REQUESTED, REFUNDED, PARTIALLY_REFUNDED} 를 옮긴 것입니다.
 * 업무 상태라서 {@code common/EntityStatus} 를 쓰지 않고 결제 도메인 enum 으로 따로 둡니다.
 */
public enum PaymentStatus {

	/** 결제 완료 */
	COMPLETED,

	/** 결제 실패 */
	FAILED,

	/** 환불 요청 */
	REFUND_REQUESTED,

	/** 환불 완료 */
	REFUNDED,

	/** 부분 환불 */
	PARTIALLY_REFUNDED
}
