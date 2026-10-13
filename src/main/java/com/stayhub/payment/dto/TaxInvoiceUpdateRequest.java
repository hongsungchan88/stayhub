package com.stayhub.payment.dto;

import com.stayhub.payment.entity.TaxInvoiceIssueType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 세금 계산서/현금 영수증 발행 요청 수정 (PUT /api/tax-invoices/{id} 의 본문).
 *
 * <p>등록 요청과 같은 모양입니다. PUT 은 "통째로 바꾸기" 라서 모든 값을 다시 보냅니다.
 * 발행 상태(issueStatus)·발행 완료 일시(issuedAt)는 이 요청으로 바꾸지 않습니다.
 */
public record TaxInvoiceUpdateRequest(

		@NotNull(message = "결제를 선택해 주세요.")
		Long paymentId,

		@NotNull(message = "발행 유형을 선택해 주세요.")
		TaxInvoiceIssueType issueType,

		// 세금 계산서는 필수 (서비스에서 확인). 현금 영수증은 비웁니다.
		@Size(max = 20, message = "사업자 등록 번호는 20자 이내로 입력해 주세요.")
		String businessNumber,

		// 선택 입력.
		@Size(max = 100, message = "상호명은 100자 이내로 입력해 주세요.")
		String businessName,

		// 선택 입력. 현금 영수증 발행용(휴대 전화 번호 등).
		@Size(max = 20, message = "고객 식별 번호는 20자 이내로 입력해 주세요.")
		String customerIdNumber,

		// 선택 입력.
		@Size(max = 255, message = "오류 내역은 255자 이내로 입력해 주세요.")
		String errorNote
) {
}
