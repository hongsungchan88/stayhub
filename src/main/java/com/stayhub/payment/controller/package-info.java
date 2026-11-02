/**
 * 결제 도메인의 controller 패키지.
 *
 * <p>HTTP 요청을 받고 응답을 돌려주는 곳입니다. 비즈니스 로직은 service 로 넘깁니다.
 *
 * <p>들어 있는 파일
 * <ul>
 *   <li>{@code PaymentController} — 결제/환불 API 5개 (목록·상세·합계·등록·수정)</li>
 *   <li>{@code AdditionalChargeController} — 추가 요금 API 4개 (목록·상세·등록·수정)</li>
 *   <li>{@code TaxInvoiceController} — 세금 계산서/현금 영수증 API 5개 (목록·상세·등록·수정·발행 상태 변경)</li>
 * </ul>
 */
package com.stayhub.payment.controller;
