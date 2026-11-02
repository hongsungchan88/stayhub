package com.stayhub.payment.dto;

import com.stayhub.payment.entity.TaxInvoiceIssueStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 세금 계산서/현금 영수증 발행 상태 변경 요청 (PUT /api/tax-invoices/{id}/status 의 본문). PAY-004
 *
 * <p>{@code errorNote} 는 발행 실패(FAILED)로 바꿀 때 필수입니다 (서비스에서 확인).
 */
public record TaxInvoiceStatusChangeRequest(

		@NotNull(message = "변경 후 상태를 선택해 주세요.")
		TaxInvoiceIssueStatus newStatus,

		@Size(max = 255, message = "오류 내역은 255자 이내로 입력해 주세요.")
		String errorNote
) {
}
