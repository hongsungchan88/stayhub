package com.stayhub.room.entity;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

/**
 * 동적 가격 규칙 (테이블 {@code price_rules}). [v2 이관 · 스키마만 유지]
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>날짜·요일·성수기·잔여 객실 수 등 조건별 객실 요금 산출 규칙을 담습니다. (ROM-002)
 * {@code created_at} / {@code updated_at} 은 {@link BaseEntity} 를 상속해서 자동으로 채워집니다.
 *
 * <p>1차는 테이블만 만들어 둡니다. 서비스·API 는 v2 에서 만들고, 그때 아래 메서드를 그대로 씁니다.
 * <ul>
 *   <li>새로 만들 때 — {@link #create}</li>
 *   <li>고칠 때 — {@link #update}</li>
 * </ul>
 * status 컬럼이 없는 유형 ③ 테이블이라 삭제 기능은 없습니다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 가 쓰는 빈 생성자. 직접 new 하지 말고 create() 를 쓰세요.
@Entity
@Table(name = "price_rules")
public class PriceRule extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY) // AUTO_INCREMENT
	@Column(name = "rule_id")
	private Long id;

	/** 적용 객실 타입. null 이면 전체 객실 타입에 적용. rooms.room_type 과 같은 문자열입니다. */
	@Column(name = "room_type", length = 20)
	private String roomType;

	@Enumerated(EnumType.STRING)
	@Column(name = "rule_type", nullable = false, columnDefinition = "VARCHAR(20)")
	private PriceRuleType ruleType;

	/** 조건값. 예: 금요일, 2026-08-01~08-31, 잔여율 20% 이하 */
	@Column(name = "condition_value", nullable = false, length = 50)
	private String conditionValue;

	@Enumerated(EnumType.STRING)
	@ColumnDefault("'PERCENT'") // 정의서의 기본값. 문자열이라 작은따옴표로 한 번 더 감쌉니다.
	@Column(name = "adjust_type", nullable = false, columnDefinition = "VARCHAR(10)")
	private PriceRuleAdjustType adjustType;

	/** 할증/할인율 또는 조정 금액. */
	@Column(name = "adjust_value", nullable = false, precision = 6, scale = 2) // DECIMAL(6,2)
	private BigDecimal adjustValue;

	/** 최소 가격 보정값. 없으면 null. */
	@Column(name = "min_price", precision = 10, scale = 2) // DECIMAL(10,2)
	private BigDecimal minPrice;

	/** 최대 가격 보정값. 없으면 null. */
	@Column(name = "max_price", precision = 10, scale = 2) // DECIMAL(10,2)
	private BigDecimal maxPrice;

	/** 기본 요금 규칙 여부. 성수기 캘린더 미설정 시 이 규칙을 적용합니다. */
	@ColumnDefault("false") // 정의서의 "기본 FALSE"
	@Column(name = "is_default", nullable = false)
	private boolean isDefault;

	/** 규칙 적용 우선순위. 숫자가 클수록 우선. */
	@ColumnDefault("0") // 정의서의 "기본 0"
	@Column(name = "priority", nullable = false)
	private int priority;

	/**
	 * 새 가격 규칙을 만듭니다.
	 *
	 * @param adjustType 비어 있으면 PERCENT
	 * @param isDefault  비어 있으면 false
	 * @param priority   비어 있으면 0
	 */
	public static PriceRule create(
			String roomType,
			PriceRuleType ruleType,
			String conditionValue,
			PriceRuleAdjustType adjustType,
			BigDecimal adjustValue,
			BigDecimal minPrice,
			BigDecimal maxPrice,
			Boolean isDefault,
			Integer priority
	) {
		PriceRule priceRule = new PriceRule();
		priceRule.roomType = roomType;
		priceRule.ruleType = ruleType;
		priceRule.conditionValue = conditionValue;
		priceRule.adjustType = defaultAdjustType(adjustType);
		priceRule.adjustValue = adjustValue;
		priceRule.minPrice = minPrice;
		priceRule.maxPrice = maxPrice;
		priceRule.isDefault = defaultIsDefault(isDefault);
		priceRule.priority = defaultPriority(priority);
		return priceRule;
	}

	/** 내용을 고칩니다. */
	public void update(
			String roomType,
			PriceRuleType ruleType,
			String conditionValue,
			PriceRuleAdjustType adjustType,
			BigDecimal adjustValue,
			BigDecimal minPrice,
			BigDecimal maxPrice,
			Boolean isDefault,
			Integer priority
	) {
		this.roomType = roomType;
		this.ruleType = ruleType;
		this.conditionValue = conditionValue;
		this.adjustType = defaultAdjustType(adjustType);
		this.adjustValue = adjustValue;
		this.minPrice = minPrice;
		this.maxPrice = maxPrice;
		this.isDefault = defaultIsDefault(isDefault);
		this.priority = defaultPriority(priority);
	}

	/** 정의서의 기본값 "PERCENT" — 비어 있으면 PERCENT 를 넣습니다. */
	private static PriceRuleAdjustType defaultAdjustType(PriceRuleAdjustType adjustType) {
		return adjustType == null ? PriceRuleAdjustType.PERCENT : adjustType;
	}

	/** 정의서의 "기본 FALSE" — 비어 있으면 false 를 넣습니다. */
	private static boolean defaultIsDefault(Boolean isDefault) {
		return isDefault != null && isDefault;
	}

	/** 정의서의 "기본 0" — 비어 있으면 0 을 넣습니다. */
	private static int defaultPriority(Integer priority) {
		return priority == null ? 0 : priority;
	}
}
