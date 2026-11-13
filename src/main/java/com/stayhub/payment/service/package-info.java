/**
 * 결제 도메인의 service 패키지.
 *
 * <p>비즈니스 로직을 담는 곳입니다. 대부분의 코드가 여기에 쌓입니다.
 *
 * <p>들어 있는 파일
 * <ul>
 *   <li>{@code PaymentService} — 결제/환불 업무 로직과 트랜잭션. 결제 합계(PAY-002), 환불 규정 적용(PAY-001), 채널 수수료·실매출 계산(PAY-003) 포함</li>
 *   <li>{@code AdditionalChargeService} — 추가 요금 업무 로직과 트랜잭션</li>
 *   <li>{@code TaxInvoiceService} — 세금 계산서/현금 영수증 업무 로직과 트랜잭션</li>
 * </ul>
 */
package com.stayhub.payment.service;
