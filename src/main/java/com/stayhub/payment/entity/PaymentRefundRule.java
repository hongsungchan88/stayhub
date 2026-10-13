package com.stayhub.payment.entity;

/**
 * 적용된 환불 규정 (PAY-001).
 *
 * <p>테이블 정의서의 {@code refund_rule_applied VARCHAR(20) — FULL, PARTIAL, NONE} 을 옮긴 것입니다.
 */
public enum PaymentRefundRule {

	/** 전액 */
	FULL,

	/** 부분 */
	PARTIAL,

	/** 없음 */
	NONE
}
