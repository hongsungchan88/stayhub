package com.stayhub.payment.dto;

import com.stayhub.payment.entity.TaxInvoice;
import com.stayhub.payment.entity.TaxInvoiceIssueStatus;
import com.stayhub.payment.entity.TaxInvoiceIssueType;
import java.time.LocalDateTime;

/**
 * 세금 계산서/현금 영수증 응답 (목록·상세·등록·수정이 모두 이 모양으로 나갑니다).
 *
 * <p>발행 상태(issueStatus)는 업무 상태라서, paymentId 는 화면에 필요한 참조 ID 라서 넣습니다.
 * 변환({@link #from})은 서비스 안에서 호출합니다. 컨트롤러는 엔티티를 만지지 않습니다.
 */
public record TaxInvoiceResponse(
		Long id,
		Long paymentId,
		TaxInvoiceIssueType issueType,
		String businessNumber,
		String businessName,
		String customerIdNumber,
		TaxInvoiceIssueStatus issueStatus,
		String errorNote,
		LocalDateTime issuedAt,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {

	/** 엔티티를 응답 모양으로 바꿉니다. */
	public static TaxInvoiceResponse from(TaxInvoice taxInvoice) {
		return new TaxInvoiceResponse(
				taxInvoice.getId(),
				taxInvoice.getPaymentId(),
				taxInvoice.getIssueType(),
				taxInvoice.getBusinessNumber(),
				taxInvoice.getBusinessName(),
				taxInvoice.getCustomerIdNumber(),
				taxInvoice.getIssueStatus(),
				taxInvoice.getErrorNote(),
				taxInvoice.getIssuedAt(),
				taxInvoice.getCreatedAt(),
				taxInvoice.getUpdatedAt()
		);
	}
}
