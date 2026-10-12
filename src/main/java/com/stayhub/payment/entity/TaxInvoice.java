package com.stayhub.payment.entity;

import com.stayhub.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

/**
 * 세금 계산서/현금 영수증 (테이블 {@code tax_invoices}).
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>결제 완료 건에 대한 세금 계산서·현금 영수증 발행 요청과 처리 내역을 담습니다. (PAY-004)
 * {@code created_at} / {@code updated_at} 은 {@link BaseEntity} 를 상속해서 자동으로 채워집니다.
 *
 * <p>값을 바꾸는 방법은 아래 메서드뿐입니다.
 * <ul>
 *   <li>새로 만들 때 — {@link #create}</li>
 *   <li>고칠 때 — {@link #update}</li>
 * </ul>
 * 삭제 기능은 없습니다.
 *
 * <p><b>필드명 규칙</b> — 자기 PK 는 {@code id}, 다른 테이블을 가리키는 값은 {@code xxxId}.
 * 예) {@code paymentId}(payments)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 가 쓰는 빈 생성자. 직접 new 하지 말고 create() 를 쓰세요.
@Entity
@Table(name = "tax_invoices")
public class TaxInvoice extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY) // AUTO_INCREMENT
	@Column(name = "invoice_id")
	private Long id;

	/** 대상 결제 ID. payments 테이블을 가리킵니다. {@code @ManyToOne} 을 쓰지 않고 값(Long)만 들고 있습니다. */
	@Column(name = "payment_id", nullable = false)
	private Long paymentId;

	@Enumerated(EnumType.STRING)
	@Column(name = "issue_type", nullable = false, columnDefinition = "VARCHAR(20)")
	private TaxInvoiceIssueType issueType;

	/** 사업자 등록 번호. 세금 계산서는 필수, 현금 영수증은 null. */
	@Column(name = "business_number", length = 20)
	private String businessNumber;

	/** 사업자(상호)명. 없으면 null. */
	@Column(name = "business_name", length = 100)
	private String businessName;

	/** 고객 식별 번호. 현금 영수증 발행용(휴대 전화 번호 등). 없으면 null. */
	@Column(name = "customer_id_number", length = 20)
	private String customerIdNumber;

	@Enumerated(EnumType.STRING)
	@ColumnDefault("'REQUESTED'") // 정의서의 기본값. 문자열이라 작은따옴표로 한 번 더 감쌉니다.
	@Column(name = "issue_status", nullable = false, columnDefinition = "VARCHAR(20)")
	private TaxInvoiceIssueStatus issueStatus;

	/** 오류 내역. 사업자 정보 누락·발행 시스템 연동 오류 등. 없으면 null. */
	@Column(name = "error_note", length = 255)
	private String errorNote;

	/** 발행 완료 일시. 아직 발행 전이면 null. */
	@Column(name = "issued_at")
	private LocalDateTime issuedAt;

	/** 새 발행 요청을 만듭니다. 상태는 항상 REQUESTED 로 시작합니다. */
	public static TaxInvoice create(
			Long paymentId,
			TaxInvoiceIssueType issueType,
			String businessNumber,
			String businessName,
			String customerIdNumber,
			String errorNote
	) {
		TaxInvoice taxInvoice = new TaxInvoice();
		taxInvoice.paymentId = paymentId;
		taxInvoice.issueType = issueType;
		taxInvoice.businessNumber = businessNumber;
		taxInvoice.businessName = businessName;
		taxInvoice.customerIdNumber = customerIdNumber;
		taxInvoice.errorNote = errorNote;
		taxInvoice.issueStatus = TaxInvoiceIssueStatus.REQUESTED;
		return taxInvoice;
	}

	/** 내용을 고칩니다. issueStatus 와 issuedAt 은 여기서 바꾸지 않습니다. */
	public void update(
			Long paymentId,
			TaxInvoiceIssueType issueType,
			String businessNumber,
			String businessName,
			String customerIdNumber,
			String errorNote
	) {
		this.paymentId = paymentId;
		this.issueType = issueType;
		this.businessNumber = businessNumber;
		this.businessName = businessName;
		this.customerIdNumber = customerIdNumber;
		this.errorNote = errorNote;
	}
}
