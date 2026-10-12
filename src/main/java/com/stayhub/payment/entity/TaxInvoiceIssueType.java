package com.stayhub.payment.entity;

/**
 * 발행 유형 (PAY-004).
 *
 * <p>테이블 정의서의 {@code issue_type VARCHAR(20) — TAX_INVOICE, CASH_RECEIPT} 를 옮긴 것입니다.
 */
public enum TaxInvoiceIssueType {

	/** 세금 계산서 */
	TAX_INVOICE,

	/** 현금 영수증 */
	CASH_RECEIPT
}
