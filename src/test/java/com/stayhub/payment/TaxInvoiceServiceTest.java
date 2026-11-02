package com.stayhub.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stayhub.common.BusinessException;
import com.stayhub.common.PageResponse;
import com.stayhub.payment.dto.TaxInvoiceCreateRequest;
import com.stayhub.payment.dto.TaxInvoiceResponse;
import com.stayhub.payment.dto.TaxInvoiceStatusChangeRequest;
import com.stayhub.payment.dto.TaxInvoiceUpdateRequest;
import com.stayhub.payment.entity.Payment;
import com.stayhub.payment.entity.PaymentStatus;
import com.stayhub.payment.entity.TaxInvoiceIssueStatus;
import com.stayhub.payment.entity.TaxInvoiceIssueType;
import com.stayhub.payment.repository.PaymentRepository;
import com.stayhub.payment.repository.TaxInvoiceRepository;
import com.stayhub.payment.service.TaxInvoiceService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
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

	@Autowired
	private PaymentRepository paymentRepository;

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

	@Test
	@DisplayName("결제가 완료 상태면 발행되고 발행 일시가 남는다")
	void issueWhenPaymentCompleted() {
		TaxInvoiceResponse created = invoiceOf(savePayment(PaymentStatus.COMPLETED));

		TaxInvoiceResponse issued = change(created.id(), TaxInvoiceIssueStatus.ISSUED, null);

		assertThat(issued.issueStatus()).isEqualTo(TaxInvoiceIssueStatus.ISSUED);
		assertThat(issued.issuedAt()).isNotNull();
	}

	@Test
	@DisplayName("결제가 완료 상태가 아니면 발행할 수 없고 상태도 그대로다")
	void issueWhenPaymentNotCompletedThrows() {
		TaxInvoiceResponse created = invoiceOf(savePayment(PaymentStatus.REFUNDED));

		assertThatThrownBy(() -> change(created.id(), TaxInvoiceIssueStatus.ISSUED, null))
				.isInstanceOf(BusinessException.class)
				.hasMessage("결제 완료 상태에서만 발행할 수 있습니다.");

		assertThat(taxInvoiceService.getDetail(created.id()).issueStatus()).isEqualTo(TaxInvoiceIssueStatus.REQUESTED);
	}

	@Test
	@DisplayName("없는 결제의 문서는 발행할 수 없다")
	void issueWithMissingPaymentThrows() {
		TaxInvoiceResponse created = invoiceOf(99999L);

		assertThatThrownBy(() -> change(created.id(), TaxInvoiceIssueStatus.ISSUED, null))
				.isInstanceOf(BusinessException.class)
				.hasMessage("발행할 결제 내역을 찾을 수 없습니다.");
	}

	@Test
	@DisplayName("실패에는 사유가 필수이고, 재시도 후 발행되면 오류 내역이 지워진다")
	void failThenRetryThenIssue() {
		TaxInvoiceResponse created = invoiceOf(savePayment(PaymentStatus.COMPLETED));

		assertThatThrownBy(() -> change(created.id(), TaxInvoiceIssueStatus.FAILED, " "))
				.isInstanceOf(BusinessException.class)
				.hasMessage("발행 실패 사유를 입력해 주세요.");

		TaxInvoiceResponse failed = change(created.id(), TaxInvoiceIssueStatus.FAILED, "발행 시스템 연동 오류");
		assertThat(failed.issueStatus()).isEqualTo(TaxInvoiceIssueStatus.FAILED);
		assertThat(failed.errorNote()).isEqualTo("발행 시스템 연동 오류");
		assertThat(failed.issuedAt()).isNull();

		TaxInvoiceResponse retried = change(created.id(), TaxInvoiceIssueStatus.REQUESTED, null);
		assertThat(retried.issueStatus()).isEqualTo(TaxInvoiceIssueStatus.REQUESTED);
		assertThat(retried.errorNote()).isEqualTo("발행 시스템 연동 오류");

		TaxInvoiceResponse issued = change(created.id(), TaxInvoiceIssueStatus.ISSUED, null);
		assertThat(issued.errorNote()).isNull();
	}

	@Test
	@DisplayName("허용하지 않은 발행 상태 변경은 BusinessException 이 난다")
	void invalidTransitionsThrow() {
		String message = "발행 상태를 바꿀 수 없습니다. 요청 → 발행 완료 또는 실패, 실패 → 재요청만 가능합니다.";
		TaxInvoiceResponse created = invoiceOf(savePayment(PaymentStatus.COMPLETED));

		assertThatThrownBy(() -> change(created.id(), TaxInvoiceIssueStatus.REQUESTED, null)) // 같은 상태
				.isInstanceOf(BusinessException.class).hasMessage(message);

		change(created.id(), TaxInvoiceIssueStatus.FAILED, "오류");
		assertThatThrownBy(() -> change(created.id(), TaxInvoiceIssueStatus.ISSUED, null)) // 실패에서 바로 발행
				.isInstanceOf(BusinessException.class).hasMessage(message);

		change(created.id(), TaxInvoiceIssueStatus.REQUESTED, null);
		change(created.id(), TaxInvoiceIssueStatus.ISSUED, null);
		assertThatThrownBy(() -> change(created.id(), TaxInvoiceIssueStatus.FAILED, "오류")) // 발행 후 변경
				.isInstanceOf(BusinessException.class).hasMessage(message);
	}

	@Test
	@DisplayName("발행 완료된 문서는 수정할 수 없다")
	void updateAfterIssuedThrows() {
		TaxInvoiceResponse created = invoiceOf(savePayment(PaymentStatus.COMPLETED));
		change(created.id(), TaxInvoiceIssueStatus.ISSUED, null);

		assertThatThrownBy(() -> taxInvoiceService.update(created.id(), new TaxInvoiceUpdateRequest(
				2L, TaxInvoiceIssueType.CASH_RECEIPT, null, null, "010-0000-0000", null)))
				.isInstanceOf(BusinessException.class)
				.hasMessage("발행 완료된 문서는 수정할 수 없습니다.");
	}

	private TaxInvoiceResponse change(Long id, TaxInvoiceIssueStatus newStatus, String errorNote) {
		return taxInvoiceService.changeStatus(id, new TaxInvoiceStatusChangeRequest(newStatus, errorNote));
	}

	private TaxInvoiceResponse invoiceOf(Long paymentId) {
		return taxInvoiceService.create(new TaxInvoiceCreateRequest(
				paymentId, TaxInvoiceIssueType.CASH_RECEIPT, null, null, "010-1234-5678", null));
	}

	/** 원하는 상태의 결제를 저장합니다. 상태를 바꾸는 메서드가 아직 없어(PAY-001) 테스트에서만 값을 넣습니다. */
	private Long savePayment(PaymentStatus status) {
		Payment payment = Payment.create(1L, 1L, "사전 결제", new BigDecimal("100000.00"), null, null, null);
		ReflectionTestUtils.setField(payment, "paymentStatus", status);
		return paymentRepository.save(payment).getId();
	}

	/** 테스트용 등록 요청 — 현금 영수증이라 사업자 정보 없이 고객 휴대 전화 번호만. */
	private TaxInvoiceCreateRequest cashReceiptRequest() {
		return new TaxInvoiceCreateRequest(
				1L, TaxInvoiceIssueType.CASH_RECEIPT, null, null, "010-1234-5678", null);
	}
}
