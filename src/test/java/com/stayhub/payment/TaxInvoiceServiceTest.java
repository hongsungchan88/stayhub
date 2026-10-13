package com.stayhub.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stayhub.common.BusinessException;
import com.stayhub.common.PageResponse;
import com.stayhub.payment.dto.TaxInvoiceCreateRequest;
import com.stayhub.payment.dto.TaxInvoiceResponse;
import com.stayhub.payment.dto.TaxInvoiceUpdateRequest;
import com.stayhub.payment.entity.TaxInvoiceIssueStatus;
import com.stayhub.payment.entity.TaxInvoiceIssueType;
import com.stayhub.payment.repository.TaxInvoiceRepository;
import com.stayhub.payment.service.TaxInvoiceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 세금 계산서/현금 영수증 서비스 테스트.
 *
 * <p>실제 DB(application-local.yml 의 MySQL)에 붙어서 등록·조회·수정이 제대로 되는지 확인합니다.
 * {@code @Transactional} 이 붙어 있어서 테스트가 끝나면 넣었던 데이터가 전부 되돌려집니다(롤백).
 * 삭제 기능이 없는 테이블이라 삭제 테스트는 없습니다.
 */
@SpringBootTest
@Transactional
class TaxInvoiceServiceTest {

	@Autowired
	private TaxInvoiceService taxInvoiceService;

	@Autowired
	private TaxInvoiceRepository taxInvoiceRepository;

	@BeforeEach
	void setUp() {
		// 개발 DB 에 이미 들어 있는 데이터 때문에 결과가 흔들리지 않도록 테스트 안에서만 비웁니다.
		// @Transactional 이라 테스트가 끝나면 지운 것까지 원래대로 돌아옵니다.
		// 경고: @Transactional을 지우면 개발 DB의 이 테이블이 실제로 비워집니다. 지우지 마세요.
		taxInvoiceRepository.deleteAll();
	}

	@Test
	@DisplayName("등록하면 목록에 나온다")
	void createThenAppearsInList() {
		TaxInvoiceResponse created = taxInvoiceService.create(cashReceiptRequest());

		PageResponse<TaxInvoiceResponse> all = taxInvoiceService.getList(0);
		assertThat(all.content()).extracting(TaxInvoiceResponse::id).containsExactly(created.id());

		// 상태는 REQUESTED, 발행 완료 일시는 비어 있다
		assertThat(created.issueStatus()).isEqualTo(TaxInvoiceIssueStatus.REQUESTED);
		assertThat(created.issuedAt()).isNull();
	}

	@Test
	@DisplayName("수정하면 바뀐 값이 조회된다")
	void updateIsApplied() {
		TaxInvoiceResponse created = taxInvoiceService.create(cashReceiptRequest());

		taxInvoiceService.update(created.id(), new TaxInvoiceUpdateRequest(
				2L, TaxInvoiceIssueType.TAX_INVOICE, "123-45-67890", "스테이허브상사", null, null));

		TaxInvoiceResponse found = taxInvoiceService.getDetail(created.id());
		assertThat(found.paymentId()).isEqualTo(2L);
		assertThat(found.issueType()).isEqualTo(TaxInvoiceIssueType.TAX_INVOICE);
		assertThat(found.businessNumber()).isEqualTo("123-45-67890");
		assertThat(found.businessName()).isEqualTo("스테이허브상사");
		assertThat(found.customerIdNumber()).isNull();
	}

	@Test
	@DisplayName("세금 계산서인데 사업자 등록 번호가 없으면 BusinessException 이 난다")
	void taxInvoiceWithoutBusinessNumberThrows() {
		assertThatThrownBy(() -> taxInvoiceService.create(new TaxInvoiceCreateRequest(
				1L, TaxInvoiceIssueType.TAX_INVOICE, null, "스테이허브상사", null, null)))
				.isInstanceOf(BusinessException.class)
				.hasMessage("세금 계산서는 사업자 등록 번호를 입력해야 합니다.");
	}

	@Test
	@DisplayName("없는 항목을 상세 조회하면 BusinessException 이 난다")
	void getMissingThrows() {
		assertThatThrownBy(() -> taxInvoiceService.getDetail(99999L))
				.isInstanceOf(BusinessException.class)
				.hasMessage("세금 계산서/현금 영수증을 찾을 수 없습니다.");
	}

	/** 테스트용 등록 요청 — 현금 영수증이라 사업자 정보 없이 고객 휴대 전화 번호만. */
	private TaxInvoiceCreateRequest cashReceiptRequest() {
		return new TaxInvoiceCreateRequest(
				1L, TaxInvoiceIssueType.CASH_RECEIPT, null, null, "010-1234-5678", null);
	}
}
