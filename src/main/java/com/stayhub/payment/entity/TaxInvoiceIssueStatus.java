package com.stayhub.payment.entity;

/**
 * 발행 상태 (PAY-004).
 *
 * <p>테이블 정의서의 {@code issue_status VARCHAR(20) — REQUESTED, ISSUED, FAILED} 를 옮긴 것입니다.
 * 업무 상태라서 {@code common/EntityStatus} 를 쓰지 않고 결제 도메인 enum 으로 따로 둡니다.
 */
public enum TaxInvoiceIssueStatus {

	/** 발행 요청 */
	REQUESTED,

	/** 발행 완료 */
	ISSUED,

	/** 발행 실패 */
	FAILED
}
