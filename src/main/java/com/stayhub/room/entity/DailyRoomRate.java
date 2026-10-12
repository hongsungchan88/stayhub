package com.stayhub.room.entity;

import com.stayhub.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

/**
 * 일별 객실 요금표 (테이블 {@code daily_room_rates}). [v2 이관 · 스키마만 유지]
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>가격 규칙으로 산출한 날짜별·객실별 최종 요금을 담는 결과 테이블입니다. (ROM-002)
 * {@code created_at} / {@code updated_at} 은 {@link BaseEntity} 를 상속해서 자동으로 채워집니다.
 * 요금을 산출·재계산한 시각은 따로 {@code generated_at} 에 남깁니다.
 *
 * <p>1차는 테이블만 만들어 둡니다. 서비스·API 는 v2 에서 만들고, 그때 아래 메서드를 그대로 씁니다.
 * <ul>
 *   <li>새로 산출할 때 — {@link #create}</li>
 *   <li>재계산할 때 — {@link #update}</li>
 * </ul>
 * status 컬럼이 없는 유형 ③ 테이블이라 삭제 기능은 없습니다.
 *
 * <p><b>UNIQUE(room_id, rate_date)</b> — 한 객실의 같은 날짜 요금은 한 줄뿐입니다.
 * 두 컬럼을 묶은 UNIQUE 라서 {@code @Column(unique = true)} 로는 표현할 수 없어 {@code @Table} 에 적습니다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 가 쓰는 빈 생성자. 직접 new 하지 말고 create() 를 쓰세요.
@Entity
@Table(
		name = "daily_room_rates",
		uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "rate_date"})
)
public class DailyRoomRate extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY) // AUTO_INCREMENT
	@Column(name = "rate_id")
	private Long id;

	/** 대상 객실 ID. rooms 테이블을 가리킵니다. {@code @ManyToOne} 을 쓰지 않고 값(Long)만 들고 있습니다. */
	@Column(name = "room_id", nullable = false)
	private Long roomId;

	@Column(name = "rate_date", nullable = false)
	private LocalDate rateDate;

	/** 기본 요금. 산출 시점의 rooms.base_price 값. */
	@Column(name = "base_price", nullable = false, precision = 10, scale = 2) // DECIMAL(10,2)
	private BigDecimal basePrice;

	/** 최종 산출 요금. */
	@Column(name = "final_price", nullable = false, precision = 10, scale = 2) // DECIMAL(10,2)
	private BigDecimal finalPrice;

	/** 적용된 가격 규칙 ID. price_rules 테이블을 가리킵니다. 적용된 규칙이 없으면 null. */
	@Column(name = "applied_rule_id")
	private Long appliedRuleId;

	/** 산출 시점 잔여 객실 수. */
	@Column(name = "remaining_rooms", nullable = false)
	private int remainingRooms;

	/** 요금 갱신(재계산) 일시. 산출·재계산할 때마다 지금 시각으로 바뀝니다. */
	// 정의서의 기본값 CURRENT_TIMESTAMP. 컬럼이 datetime(6)(소수점 6자리 초)으로 만들어져서
	// MySQL 이 기본값도 같은 자릿수인 CURRENT_TIMESTAMP(6) 만 받습니다. (괄호를 빼면 테이블 생성이 실패)
	@ColumnDefault("CURRENT_TIMESTAMP(6)")
	@Column(name = "generated_at", nullable = false)
	private LocalDateTime generatedAt;

	/** 새 요금을 산출해 만듭니다. generatedAt 은 지금 시각. */
	public static DailyRoomRate create(
			Long roomId,
			LocalDate rateDate,
			BigDecimal basePrice,
			BigDecimal finalPrice,
			Long appliedRuleId,
			int remainingRooms
	) {
		DailyRoomRate dailyRoomRate = new DailyRoomRate();
		dailyRoomRate.roomId = roomId;
		dailyRoomRate.rateDate = rateDate;
		dailyRoomRate.basePrice = basePrice;
		dailyRoomRate.finalPrice = finalPrice;
		dailyRoomRate.appliedRuleId = appliedRuleId;
		dailyRoomRate.remainingRooms = remainingRooms;
		dailyRoomRate.generatedAt = LocalDateTime.now();
		return dailyRoomRate;
	}

	/** 요금을 재계산한 값으로 고칩니다. roomId·rateDate 는 바꾸지 않고, generatedAt 은 지금 시각으로. */
	public void update(
			BigDecimal basePrice,
			BigDecimal finalPrice,
			Long appliedRuleId,
			int remainingRooms
	) {
		this.basePrice = basePrice;
		this.finalPrice = finalPrice;
		this.appliedRuleId = appliedRuleId;
		this.remainingRooms = remainingRooms;
		this.generatedAt = LocalDateTime.now();
	}
}
