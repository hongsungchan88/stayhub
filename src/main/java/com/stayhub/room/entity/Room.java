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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

/**
 * 객실 (테이블 {@code rooms}).
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>객실 기본 정보와 현재 객실 상태(빈방/청소필요/청소중/투숙중)를 담습니다.
 * {@code created_at} / {@code updated_at} 은 {@link BaseEntity} 를 상속해서 자동으로 채워집니다.
 *
 * <p>값을 바꾸는 방법은 아래 메서드뿐입니다.
 * <ul>
 *   <li>새로 만들 때 — {@link #create}</li>
 *   <li>고칠 때 — {@link #update}</li>
 * </ul>
 * 유형 ②(업무 상태 있음) 테이블이라 삭제 기능은 없습니다.
 *
 * <p><b>필드명 규칙</b> — 자기 PK 는 {@code id}, 다른 테이블을 가리키는 값은 {@code xxxId}.
 * 예) {@code propertyId}, {@code statusUpdatedBy}(users)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 가 쓰는 빈 생성자. 직접 new 하지 말고 create() 를 쓰세요.
@Entity
@Table(name = "rooms")
public class Room extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY) // AUTO_INCREMENT
	@Column(name = "room_id")
	private Long id;

	/**
	 * 숙소 ID. properties 테이블을 가리킵니다.
	 * {@code @ManyToOne} 을 쓰지 않고 값(Long)만 들고 있습니다.
	 * 등록할 때 {@code PropertyScope} 값을 넣고, 조회 조건에는 넣지 않습니다(정책 32행, W-107).
	 */
	@ColumnDefault("1") // 정의서의 "기본 1"
	@Column(name = "property_id", nullable = false)
	private Long propertyId;

	@Column(name = "room_number", nullable = false, length = 20, unique = true) // 정의서의 UNIQUE
	private String roomNumber;

	/** 싱글/더블/스위트 등. 정의서에 값 목록이 정해져 있지 않아 enum 이 아닌 문자열로 둡니다. */
	@Column(name = "room_type", nullable = false, length = 20)
	private String roomType;

	/** 기본(정상가) 1박 요금. ROM-002 동적 가격 계산의 기준값. */
	@Column(name = "base_price", nullable = false, precision = 10, scale = 2) // DECIMAL(10,2)
	private BigDecimal basePrice;

	@Enumerated(EnumType.STRING)
	@ColumnDefault("'VACANT'") // 정의서의 기본값. 문자열이라 작은따옴표로 한 번 더 감쌉니다.
	@Column(name = "status", nullable = false, columnDefinition = "VARCHAR(20)")
	private RoomStatus status;

	/** 상태 최종 변경 담당자 ID. users 테이블을 가리킵니다. 아직 바뀐 적 없으면 null. */
	@Column(name = "status_updated_by")
	private Long statusUpdatedBy;

	/** 상태 최종 변경 일시. 아직 바뀐 적 없으면 null. */
	@Column(name = "status_updated_at")
	private LocalDateTime statusUpdatedAt;

	@ColumnDefault("1") // 정의서의 "기본 1"
	@Column(name = "max_occupancy", nullable = false)
	private int maxOccupancy;

	/**
	 * 새 객실을 만듭니다. 상태는 항상 VACANT 로 시작합니다.
	 *
	 * @param maxOccupancy 비어 있으면 1
	 */
	public static Room create(
			Long propertyId,
			String roomNumber,
			String roomType,
			BigDecimal basePrice,
			Integer maxOccupancy
	) {
		Room room = new Room();
		room.propertyId = propertyId;
		room.roomNumber = roomNumber;
		room.roomType = roomType;
		room.basePrice = basePrice;
		room.maxOccupancy = defaultMaxOccupancy(maxOccupancy);
		room.status = RoomStatus.VACANT;
		return room;
	}

	/** 기본 정보를 고칩니다. propertyId 와 status·statusUpdatedBy·statusUpdatedAt 은 여기서 바꾸지 않습니다. */
	public void update(
			String roomNumber,
			String roomType,
			BigDecimal basePrice,
			Integer maxOccupancy
	) {
		this.roomNumber = roomNumber;
		this.roomType = roomType;
		this.basePrice = basePrice;
		this.maxOccupancy = defaultMaxOccupancy(maxOccupancy);
	}

	/** 정의서의 "기본 1" — 비어 있으면 1 을 넣습니다. */
	private static int defaultMaxOccupancy(Integer maxOccupancy) {
		return maxOccupancy == null ? 1 : maxOccupancy;
	}
}
