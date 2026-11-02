/**
 * 결제 도메인의 dto 패키지.
 *
 * <p>요청·응답용 데이터 전달 객체를 두는 곳입니다. 엔티티를 그대로 노출하지 않기 위해 씁니다.
 *
 * <p>들어 있는 파일
 * <ul>
 *   <li>{@code PaymentCreateRequest} — 결제 등록 요청</li>
 *   <li>{@code PaymentUpdateRequest} — 결제 수정 요청</li>
 *   <li>{@code PaymentResponse} — 결제 목록·상세·등록·수정 응답</li>
 *   <li>{@code PaymentTotalResponse} — 결제 합계 응답 (PAY-002)</li>
 *   <li>{@code AdditionalChargeCreateRequest} — 추가 요금 등록 요청</li>
 *   <li>{@code AdditionalChargeUpdateRequest} — 추가 요금 수정 요청</li>
 *   <li>{@code AdditionalChargeResponse} — 추가 요금 목록·상세·등록·수정 응답</li>
 *   <li>{@code TaxInvoiceCreateRequest} — 세금 계산서/현금 영수증 등록 요청</li>
 *   <li>{@code TaxInvoiceUpdateRequest} — 세금 계산서/현금 영수증 수정 요청</li>
 *   <li>{@code TaxInvoiceStatusChangeRequest} — 발행 상태 변경 요청 (PAY-004)</li>
 *   <li>{@code TaxInvoiceResponse} — 세금 계산서/현금 영수증 목록·상세·등록·수정·발행 상태 변경 응답</li>
 * </ul>
 */
package com.stayhub.payment.dto;
