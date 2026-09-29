package com.stayhub.content.entity;

import com.stayhub.common.BaseEntity;
import com.stayhub.common.EntityStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

/**
 * 숙소 가이드북 (테이블 {@code guidebooks}).
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>DB 테이블 한 행을 자바 객체 하나로 표현합니다. 테이블 정의서의 컬럼이 필드 하나씩에 대응합니다.
 * {@code created_at} / {@code updated_at} 은 {@link BaseEntity} 를 상속해서 자동으로 채워집니다.
 *
 * <p>값을 바꾸는 방법은 아래 메서드뿐입니다.
 * <ul>
 *   <li>새로 만들 때 — {@link #create}</li>
 *   <li>고칠 때 — {@link #update}</li>
 *   <li>지울 때 — {@link #delete} (실제로 지우지 않고 status 만 DELETED 로. 유형 ①만)</li>
 * </ul>
 * {@code @Setter} 를 쓰지 않는 이유: 아무 곳에서나 값을 바꿀 수 있으면 어디서 바뀌었는지 추적이 안 됩니다.
 *
 * <p><b>따라 만들 때 바꿀 곳</b>
 * <ul>
 *   <li>{@code @Table(name)} — 테이블 정의서의 테이블명 (복수형)</li>
 *   <li>클래스명 — 테이블명의 단수형. 예) {@code rooms} → {@code Room}</li>
 *   <li>PK 컬럼명 — {@code guidebook_id} → {@code room_id}. 필드명은 {@code id} 그대로 둡니다</li>
 *   <li>컬럼 필드 — 정의서의 컬럼마다 {@code @Column} + 필드 한 묶음씩</li>
 *   <li>{@code create} / {@code update} 의 파라미터 — 위 필드에 맞춰서</li>
 *   <li>{@code propertyId} — {@code property_id} 가 있는 4개 테이블만 둡니다. 없으면 필드와 {@code create} 의 관련 줄을 지웁니다</li>
 *   <li>{@code status} 필드·{@code delete()} — 테이블 유형에 따라. ① 그대로 / ② {@code EntityStatus} 를 도메인 enum(예: {@code RoomStatus})으로 바꾸고 {@code delete()} 삭제 / ③ 둘 다 삭제</li>
 * </ul>
 *
 * <p><b>enum 컬럼은 반드시 {@code columnDefinition = "VARCHAR(n)"}</b>
 * <br>빼먹으면 MySQL 의 ENUM 타입으로 만들어져 테이블 정의서(VARCHAR)와 달라지고,
 * 나중에 enum 에 값을 추가했을 때 저장이 {@code Data truncated} 오류로 실패합니다.
 * ({@code ddl-auto: update} 는 이미 있는 컬럼의 타입을 고쳐주지 않습니다)
 *
 * <p><b>필드명 규칙</b> — 자기 PK 는 {@code id}, 다른 테이블을 가리키는 값은 {@code xxxId}.
 * 예) {@code propertyId}, {@code roomId}
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 가 쓰는 빈 생성자. 직접 new 하지 말고 create() 를 쓰세요.
@Entity
@Table(name = "guidebooks") // ★ 바꿀 곳: 테이블명 (정의서 그대로, 복수형)
public class Guidebook extends BaseEntity { // ★ 바꿀 곳: 클래스명 (테이블명의 단수형)

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY) // AUTO_INCREMENT
	@Column(name = "guidebook_id") // ★ 바꿀 곳: PK 컬럼명
	private Long id;

	/**
	 * 숙소 ID. properties 테이블을 가리킵니다.
	 * {@code @ManyToOne} 을 쓰지 않고 값(Long)만 들고 있습니다.
	 * open-in-view: false 설정이라 연관관계를 걸면 서비스 밖에서 지연 로딩 예외가 납니다.
	 *
	 * <p>property_id 가 있는 4개 테이블(rooms, property_images, property_facilities, guidebooks)만 해당합니다.
	 * 등록할 때 {@code PropertyScope} 값을 넣고, 조회 조건에는 넣지 않습니다(정책 32행).
	 * properties는 property_id가 자기 PK(AUTO_INCREMENT)라 해당 없음. PropertyScope를 넣지 않는다.
	 * 내 테이블에 property_id 가 없으면 이 필드와 {@code create} 의 관련 줄을 전부 지웁니다.
	 */
	@Column(name = "property_id", nullable = false) // ★ 바꿀 곳: 내 테이블에 property_id 가 없으면 이 필드째 삭제
	private Long propertyId;

	@Enumerated(EnumType.STRING)
	@Column(name = "category", nullable = false, columnDefinition = "VARCHAR(20)") // ★ 바꿀 곳: 컬럼명·길이 (enum 은 columnDefinition 필수)
	private GuidebookCategory category; // ★ 바꿀 곳: 타입·필드명

	@Column(name = "title", nullable = false, length = 50) // ★ 바꿀 곳: 컬럼명·길이
	private String title; // ★ 바꿀 곳: 필드명

	@Column(name = "content", nullable = false, columnDefinition = "TEXT") // ★ 바꿀 곳: 컬럼명 (TEXT 는 columnDefinition 으로)
	private String content; // ★ 바꿀 곳: 필드명

	@ColumnDefault("0") // 정의서의 "기본 0"
	@Column(name = "sort_order", nullable = false) // ★ 바꿀 곳: 컬럼명
	private int sortOrder; // ★ 바꿀 곳: 필드명

	@Enumerated(EnumType.STRING)
	@ColumnDefault("'ACTIVE'") // ★ 바꿀 곳: 유형 ②는 정의서의 기본값으로 (예: properties 는 'DRAFT'), ③은 삭제. 문자열이라 작은따옴표로 한 번 더 감쌉니다.
	@Column(name = "status", nullable = false, columnDefinition = "VARCHAR(20)") // ★ 바꿀 곳: 유형 ②는 정의서의 길이로, ③은 이 묶음 삭제
	private EntityStatus status; // ★ 바꿀 곳: 유형 ②는 도메인 enum(예: RoomStatus)으로, ③은 삭제

	/**
	 * 새 가이드북을 만듭니다. 상태는 항상 ACTIVE 로 시작합니다.
	 *
	 * @param sortOrder 비어 있으면 0. 0끼리는 등록 순(guidebook_id 순)으로 정렬됩니다.
	 */
	public static Guidebook create( // ★ 바꿀 곳: 파라미터를 내 테이블 컬럼에 맞게
			Long propertyId, // ★ 바꿀 곳: property_id 가 없으면 삭제
			GuidebookCategory category,
			String title,
			String content,
			Integer sortOrder
	) {
		Guidebook guidebook = new Guidebook(); // ★ 바꿀 곳: 클래스명
		guidebook.propertyId = propertyId; // ★ 바꿀 곳: property_id 가 없으면 삭제
		guidebook.category = category; // ★ 바꿀 곳: 필드
		guidebook.title = title; // ★ 바꿀 곳: 필드
		guidebook.content = content; // ★ 바꿀 곳: 필드
		guidebook.sortOrder = defaultSortOrder(sortOrder);
		guidebook.status = EntityStatus.ACTIVE; // ★ 바꿀 곳: 유형 ②는 정의서의 첫 상태값으로, ③은 삭제
		return guidebook;
	}

	/** 내용을 고칩니다. propertyId 와 status 는 여기서 바꾸지 않습니다. */
	public void update( // ★ 바꿀 곳: 파라미터를 내 테이블 컬럼에 맞게
			GuidebookCategory category,
			String title,
			String content,
			Integer sortOrder
	) {
		this.category = category; // ★ 바꿀 곳: 필드
		this.title = title; // ★ 바꿀 곳: 필드
		this.content = content; // ★ 바꿀 곳: 필드
		this.sortOrder = defaultSortOrder(sortOrder);
	}

	/** 소프트 삭제. 행은 그대로 두고 status 만 DELETED 로 바꿉니다. (유형 ①만) */
	public void delete() { // ★ 바꿀 곳: 유형 ②·③이면 이 메서드째 삭제
		this.status = EntityStatus.DELETED;
	}

	/** 정의서의 "미입력 시 등록 순" — 비어 있으면 0 을 넣습니다. */
	private static int defaultSortOrder(Integer sortOrder) {
		return sortOrder == null ? 0 : sortOrder;
	}
}
