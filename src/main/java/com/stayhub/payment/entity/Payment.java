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
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

/**
 * 결제/환불 (테이블 {@code payments}).
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>사전 결제·현장 결제 내역과 환불 처리, 채널 수수료 및 실매출을 담습니다. (PAY-001, PAY-003)
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
 * 예) {@code reservationId}(reservations), {@code channelId}(channels)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 가 쓰는 빈 생성자. 직접 new 하지 말고 create() 를 쓰세요.
@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY) // AUTO_INCREMENT
	@Column(name = "payment_id")
	private Long id;

	/** 대상 예약 ID. reservations 테이블을 가리킵니다. {@code @ManyToOne} 을 쓰지 않고 값(Long)만 들고 있습니다. */
	@Column(name = "reservation_id", nullable = false)
	private Long reservationId;

	/** 예약 채널 ID. channels 테이블을 가리킵니다. */
	@Column(name = "channel_id", nullable = false)
	private Long channelId;

	/** 결제 수단. 사전 결제/현장 결제. 정의서에 코드값이 정해져 있지 않아 enum 이 아닌 문자열로 둡니다. */
	@Column(name = "payment_method", nullable = false, length = 20)
	private String paymentMethod;

	@Column(name = "payment_amount", nullable = false, precision = 10, scale = 2) // DECIMAL(10,2)
	private BigDecimal paymentAmount;

	/** 채널 수수료 금액. */
	@ColumnDefault("0") // 정의서의 "기본 0"
	@Column(name = "commission_amount", nullable = false, precision = 10, scale = 2) // DECIMAL(10,2)
	private BigDecimal commissionAmount;

	/** 실매출(순매출) = 결제 금액 - 수수료. */
	@ColumnDefault("0") // 정의서의 "기본 0"
	@Column(name = "net_revenue", nullable = false, precision = 10, scale = 2) // DECIMAL(10,2)
	private BigDecimal netRevenue;

	@Enumerated(EnumType.STRING)
	@ColumnDefault("'COMPLETED'") // 정의서의 기본값. 문자열이라 작은따옴표로 한 번 더 감쌉니다.
	@Column(name = "payment_status", nullable = false, columnDefinition = "VARCHAR(20)")
	private PaymentStatus paymentStatus;

	/** 환불 요청일. 환불 요청 전이면 null. */
	@Column(name = "refund_request_date")
	private LocalDate refundRequestDate;

	/** 환불 금액. 환불 전이면 null. */
	@Column(name = "refund_amount", precision = 10, scale = 2) // DECIMAL(10,2)
	private BigDecimal refundAmount;

	/** 적용된 환불 규정. 환불 전이면 null. */
	@Enumerated(EnumType.STRING)
	@Column(name = "refund_rule_applied", columnDefinition = "VARCHAR(20)")
	private PaymentRefundRule refundRuleApplied;

	/** 오류 내역. 결제 실패·중복 결제·규정 외 환불 요청 등. 없으면 null. */
	@Column(name = "error_note", length = 255)
	private String errorNote;

	/**
	 * 새 결제를 만듭니다. 상태는 항상 COMPLETED 로 시작합니다.
	 *
	 * @param commissionAmount 비어 있으면 0
	 * @param netRevenue       비어 있으면 0
	 */
	public static Payment create(
			Long reservationId,
			Long channelId,
			String paymentMethod,
			BigDecimal paymentAmount,
			BigDecimal commissionAmount,
			BigDecimal netRevenue,
			String errorNote
	) {
		Payment payment = new Payment();
		payment.reservationId = reservationId;
		payment.channelId = channelId;
		payment.paymentMethod = paymentMethod;
		payment.paymentAmount = paymentAmount;
		payment.commissionAmount = defaultZero(commissionAmount);
		payment.netRevenue = defaultZero(netRevenue);
		payment.errorNote = errorNote;
		payment.paymentStatus = PaymentStatus.COMPLETED;
		return payment;
	}

	/** 내용을 고칩니다. paymentStatus 와 환불 관련 값(refund_*)은 여기서 바꾸지 않습니다. */
	public void update(
			Long reservationId,
			Long channelId,
			String paymentMethod,
			BigDecimal paymentAmount,
			BigDecimal commissionAmount,
			BigDecimal netRevenue,
			String errorNote
	) {
		this.reservationId = reservationId;
		this.channelId = channelId;
		this.paymentMethod = paymentMethod;
		this.paymentAmount = paymentAmount;
		this.commissionAmount = defaultZero(commissionAmount);
		this.netRevenue = defaultZero(netRevenue);
		this.errorNote = errorNote;
	}

	/** 정의서의 "기본 0" — 비어 있으면 0 을 넣습니다. */
	private static BigDecimal defaultZero(BigDecimal amount) {
		return amount == null ? BigDecimal.ZERO : amount;
	}
}
