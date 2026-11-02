package com.stayhub.payment.service;

import com.stayhub.common.BusinessException;
import com.stayhub.common.PageResponse;
import com.stayhub.payment.dto.TaxInvoiceCreateRequest;
import com.stayhub.payment.dto.TaxInvoiceResponse;
import com.stayhub.payment.dto.TaxInvoiceStatusChangeRequest;
import com.stayhub.payment.dto.TaxInvoiceUpdateRequest;
import com.stayhub.payment.entity.Payment;
import com.stayhub.payment.entity.PaymentStatus;
import com.stayhub.payment.entity.TaxInvoice;
import com.stayhub.payment.entity.TaxInvoiceIssueStatus;
import com.stayhub.payment.entity.TaxInvoiceIssueType;
import com.stayhub.payment.repository.PaymentRepository;
import com.stayhub.payment.repository.TaxInvoiceRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 세금 계산서/현금 영수증 업무 로직.
 *
 * <p>컨트롤러에게 요청 DTO 를 받아 엔티티를 만들거나 고치고, 결과를 응답 DTO 로 바꿔 돌려줍니다.
 * 엔티티는 이 클래스 밖으로 나가지 않습니다.
 *
 * <p><b>트랜잭션 규칙</b> — 클래스에 {@code @Transactional(readOnly = true)}, 쓰기 메서드(등록·수정·발행 상태 변경)에만 {@code @Transactional}.
 *
 * <p><b>사업자 등록 번호</b> — 정의서(PAY-004)에 "세금 계산서 발행 요청 시 필수"라고 되어 있어,
 * 세금 계산서인데 비어 있으면 등록·수정을 막습니다. 현금 영수증은 비워도 됩니다.
 */
@Service
@Transactional(readOnly = true)
public class TaxInvoiceService {

	/** 목록 정렬: invoice_id 오름차순(등록 순). */
	private static final Sort LIST_SORT = Sort.by(
			Sort.Order.asc("id")
	);

	private final TaxInvoiceRepository taxInvoiceRepository;
	private final PaymentRepository paymentRepository;

	public TaxInvoiceService(TaxInvoiceRepository taxInvoiceRepository, PaymentRepository paymentRepository) {
		this.taxInvoiceRepository = taxInvoiceRepository;
		this.paymentRepository = paymentRepository;
	}

	/**
	 * 목록. 한 페이지 20건.
	 *
	 * @param page 0부터 시작
	 */
	public PageResponse<TaxInvoiceResponse> getList(int page) {
		Pageable pageable = PageRequest.of(page, PageResponse.DEFAULT_PAGE_SIZE, LIST_SORT);
		return PageResponse.from(taxInvoiceRepository.findAll(pageable).map(TaxInvoiceResponse::from));
	}

	/** 상세. 없는 항목이면 BusinessException. */
	public TaxInvoiceResponse getDetail(Long id) {
		return TaxInvoiceResponse.from(findOrThrow(id));
	}

	/** 등록. 세금 계산서인데 사업자 등록 번호가 없으면 BusinessException. */
	@Transactional
	public TaxInvoiceResponse create(TaxInvoiceCreateRequest request) {
		checkBusinessNumber(request.issueType(), request.businessNumber());

		TaxInvoice taxInvoice = TaxInvoice.create(
				request.paymentId(),
				request.issueType(),
				request.businessNumber(),
				request.businessName(),
				request.customerIdNumber(),
				request.errorNote()
		);
		return TaxInvoiceResponse.from(taxInvoiceRepository.save(taxInvoice));
	}

	/** 수정. 없는 항목이거나, 발행 완료된 문서이거나, 세금 계산서인데 사업자 등록 번호가 없으면 BusinessException. */
	@Transactional
	public TaxInvoiceResponse update(Long id, TaxInvoiceUpdateRequest request) {
		TaxInvoice taxInvoice = findOrThrow(id);
		if (taxInvoice.getIssueStatus() == TaxInvoiceIssueStatus.ISSUED) {
			throw new BusinessException("발행 완료된 문서는 수정할 수 없습니다.");
		}
		checkBusinessNumber(request.issueType(), request.businessNumber());

		taxInvoice.update(
				request.paymentId(),
				request.issueType(),
				request.businessNumber(),
				request.businessName(),
				request.customerIdNumber(),
				request.errorNote()
		);
		// save() 를 부르지 않아도 트랜잭션이 끝날 때 바뀐 값이 자동으로 DB 에 반영됩니다. (변경 감지)
		// 다만 수정일시(updatedAt)는 반영되는 순간에 채워지므로, 응답에 새 값을 담으려고 먼저 반영합니다.
		taxInvoiceRepository.flush();
		return TaxInvoiceResponse.from(taxInvoice);
	}

	/**
	 * 발행 상태 변경 (PAY-004). 외부 발행 시스템 연동 없이 처리 결과만 기록합니다.
	 *
	 * <p>허용하는 변경은 세 가지입니다.
	 * <ul>
	 *   <li>REQUESTED → ISSUED — 결제가 완료(COMPLETED) 상태일 때만. 발행 일시를 남깁니다</li>
	 *   <li>REQUESTED → FAILED — 오류 내역 필수</li>
	 *   <li>FAILED → REQUESTED — 재시도</li>
	 * </ul>
	 * 발행 완료(ISSUED)는 더 바꿀 수 없습니다.
	 */
	@Transactional
	public TaxInvoiceResponse changeStatus(Long id, TaxInvoiceStatusChangeRequest request) {
		TaxInvoice taxInvoice = findOrThrow(id);
		TaxInvoiceIssueStatus current = taxInvoice.getIssueStatus();
		TaxInvoiceIssueStatus next = request.newStatus();

		if (current == TaxInvoiceIssueStatus.REQUESTED && next == TaxInvoiceIssueStatus.ISSUED) {
			checkPaymentCompleted(taxInvoice.getPaymentId());
			taxInvoice.markIssued();
		} else if (current == TaxInvoiceIssueStatus.REQUESTED && next == TaxInvoiceIssueStatus.FAILED) {
			if (request.errorNote() == null || request.errorNote().isBlank()) {
				throw new BusinessException("발행 실패 사유를 입력해 주세요.");
			}
			taxInvoice.markFailed(request.errorNote());
		} else if (current == TaxInvoiceIssueStatus.FAILED && next == TaxInvoiceIssueStatus.REQUESTED) {
			taxInvoice.retry();
		} else {
			throw new BusinessException("발행 상태를 바꿀 수 없습니다. 요청 → 발행 완료 또는 실패, 실패 → 재요청만 가능합니다.");
		}
		// 수정일시(updatedAt)를 응답에 담으려고 먼저 반영합니다. (update 와 같은 이유)
		taxInvoiceRepository.flush();
		return TaxInvoiceResponse.from(taxInvoice);
	}

	/** 정의서의 "결제 완료 확인 후 발행" — 완료 상태의 결제만 발행할 수 있습니다. */
	private void checkPaymentCompleted(Long paymentId) {
		Payment payment = paymentRepository.findById(paymentId)
				.orElseThrow(() -> new BusinessException("발행할 결제 내역을 찾을 수 없습니다."));
		if (payment.getPaymentStatus() != PaymentStatus.COMPLETED) {
			throw new BusinessException("결제 완료 상태에서만 발행할 수 있습니다.");
		}
	}

	/** 세금 계산서는 사업자 등록 번호가 필수입니다. */
	private void checkBusinessNumber(TaxInvoiceIssueType issueType, String businessNumber) {
		if (issueType == TaxInvoiceIssueType.TAX_INVOICE && (businessNumber == null || businessNumber.isBlank())) {
			throw new BusinessException("세금 계산서는 사업자 등록 번호를 입력해야 합니다.");
		}
	}

	/** 항목 하나를 찾습니다. 없으면 예외. */
	private TaxInvoice findOrThrow(Long id) {
		return taxInvoiceRepository.findById(id)
				.orElseThrow(() -> new BusinessException("세금 계산서/현금 영수증을 찾을 수 없습니다."));
	}
}
