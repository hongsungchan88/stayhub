package com.stayhub.payment.dto;

import com.stayhub.payment.entity.TaxInvoiceIssueType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 세금 계산서/현금 영수증 발행 요청 등록 (POST /api/tax-invoices 의 본문).
 *
 * <p>여기 없는 컬럼 — {@code invoice_id}, {@code issue_status}, {@code issued_at}, {@code created_at},
 * {@code updated_at} — 은 요청에서 받지 않습니다. 발행 상태는 REQUESTED 로 시작합니다.
 */
public record TaxInvoiceCreateRequest(

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
