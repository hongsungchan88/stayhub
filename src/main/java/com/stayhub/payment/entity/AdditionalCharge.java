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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

/**
 * 추가 요금 (테이블 {@code additional_charges}).
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>미니바, 얼리 체크인/레이트 체크아웃, 기타 부대 서비스 등 추가로 발생한 비용을 담습니다. (PAY-002)
 * {@code created_at} / {@code updated_at} 은 {@link BaseEntity} 를 상속해서 자동으로 채워집니다.
 * 요금 발생/청구 일시는 따로 {@code charged_at} 에 남깁니다.
 *
 * <p>값을 바꾸는 방법은 아래 메서드뿐입니다.
 * <ul>
 *   <li>새로 만들 때 — {@link #create}</li>
 *   <li>고칠 때 — {@link #update}</li>
 * </ul>
 * 삭제 기능은 없습니다.
 *
 * <p><b>필드명 규칙</b> — 자기 PK 는 {@code id}, 다른 테이블을 가리키는 값은 {@code xxxId}.
 * 예) {@code reservationId}(reservations), {@code paymentId}(payments)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 가 쓰는 빈 생성자. 직접 new 하지 말고 create() 를 쓰세요.
@Entity
@Table(name = "additional_charges")
public class AdditionalCharge extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY) // AUTO_INCREMENT
	@Column(name = "charge_id")
	private Long id;

	/** 대상 예약 ID. reservations 테이블을 가리킵니다. {@code @ManyToOne} 을 쓰지 않고 값(Long)만 들고 있습니다. */
	@Column(name = "reservation_id", nullable = false)
	private Long reservationId;

	@Enumerated(EnumType.STRING)
	@Column(name = "charge_type", nullable = false, columnDefinition = "VARCHAR(20)")
	private AdditionalChargeType chargeType;

	/** 상세 내역. 없으면 null. */
	@Column(name = "description", length = 255)
	private String description;

	@Column(name = "amount", nullable = false, precision = 10, scale = 2) // DECIMAL(10,2)
	private BigDecimal amount;

	/** 기존 결제 건 합산 여부. TRUE 면 paymentId 의 결제에 합산, FALSE 면 별도 청구. */
	@ColumnDefault("false") // 정의서의 "기본 FALSE"
	@Column(name = "is_merged", nullable = false)
	private boolean isMerged;

	/** 합산된 결제 ID. payments 테이블을 가리킵니다. 합산하지 않았으면 null. */
	@Column(name = "payment_id")
	private Long paymentId;

	/** 정산(청구) 완료 여부. */
	@ColumnDefault("false") // 정의서의 "기본 FALSE"
	@Column(name = "is_settled", nullable = false)
	private boolean isSettled;

	/** 요금 발생/청구 일시. 등록할 때 지금 시각으로 채웁니다. */
	// 정의서의 기본값 CURRENT_TIMESTAMP. 컬럼이 datetime(6)(소수점 6자리 초)으로 만들어져서
	// MySQL 이 기본값도 같은 자릿수인 CURRENT_TIMESTAMP(6) 만 받습니다. (괄호를 빼면 테이블 생성이 실패)
	@ColumnDefault("CURRENT_TIMESTAMP(6)")
	@Column(name = "charged_at", nullable = false)
	private LocalDateTime chargedAt;

	/**
	 * 새 추가 요금을 만듭니다. chargedAt 은 지금 시각.
	 *
	 * @param isMerged  비어 있으면 false
	 * @param isSettled 비어 있으면 false
	 */
	public static AdditionalCharge create(
			Long reservationId,
			AdditionalChargeType chargeType,
			String description,
			BigDecimal amount,
			Boolean isMerged,
			Long paymentId,
			Boolean isSettled
	) {
		AdditionalCharge additionalCharge = new AdditionalCharge();
		additionalCharge.reservationId = reservationId;
		additionalCharge.chargeType = chargeType;
		additionalCharge.description = description;
		additionalCharge.amount = amount;
		additionalCharge.isMerged = defaultFalse(isMerged);
		additionalCharge.paymentId = paymentId;
		additionalCharge.isSettled = defaultFalse(isSettled);
		additionalCharge.chargedAt = LocalDateTime.now();
		return additionalCharge;
	}

	/** 내용을 고칩니다. chargedAt 은 여기서 바꾸지 않습니다. */
	public void update(
			Long reservationId,
			AdditionalChargeType chargeType,
			String description,
			BigDecimal amount,
			Boolean isMerged,
			Long paymentId,
			Boolean isSettled
	) {
		this.reservationId = reservationId;
		this.chargeType = chargeType;
		this.description = description;
		this.amount = amount;
		this.isMerged = defaultFalse(isMerged);
		this.paymentId = paymentId;
		this.isSettled = defaultFalse(isSettled);
	}

	/** 정의서의 "기본 FALSE" — 비어 있으면 false 를 넣습니다. */
	private static boolean defaultFalse(Boolean value) {
		return value != null && value;
	}
}
