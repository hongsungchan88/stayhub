/**
 * 결제 도메인의 entity 패키지.
 *
 * <p>DB 테이블과 1:1로 대응하는 JPA 엔티티를 두는 곳입니다. BaseEntity 를 상속하세요.
 *
 * <p>들어 있는 파일
 * <ul>
 *   <li>{@code Payment} — payments 테이블 한 행</li>
 *   <li>{@code PaymentStatus} — 결제 상태 값 목록</li>
 *   <li>{@code PaymentRefundRule} — 적용된 환불 규정 값 목록</li>
 *   <li>{@code AdditionalCharge} — additional_charges 테이블 한 행</li>
 *   <li>{@code AdditionalChargeType} — 추가 요금 유형 값 목록</li>
 *   <li>{@code TaxInvoice} — tax_invoices 테이블 한 행</li>
 *   <li>{@code TaxInvoiceIssueStatus} — 발행 상태 값 목록</li>
 *   <li>{@code TaxInvoiceIssueType} — 발행 유형 값 목록</li>
 * </ul>
 */
package com.stayhub.payment.entity;
