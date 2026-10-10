package com.stayhub.content.property.entity;

import com.stayhub.common.BaseEntity;
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
 * 숙소 (테이블 {@code properties}).
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>DB 테이블 한 행을 자바 객체 하나로 표현합니다. 테이블 정의서의 컬럼이 필드 하나씩에 대응합니다.
 * {@code created_at} / {@code updated_at} 은 {@link BaseEntity} 를 상속해서 자동으로 채워집니다.
 *
 * <p>값을 바꾸는 방법은 아래 메서드뿐입니다.
 * <ul>
 *   <li>새로 만들 때 — {@link #create}</li>
 *   <li>고칠 때 — {@link #update}</li>
 * </ul>
 * 유형 ②(업무 상태 DRAFT·PUBLISHED)라서 삭제 기능({@code delete()})이 없습니다.
 *
 * <p>{@code property_id} 는 이 테이블의 자기 PK(AUTO_INCREMENT)입니다. {@code PropertyScope} 값을 넣지 않습니다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 가 쓰는 빈 생성자. 직접 new 하지 말고 create() 를 쓰세요.
@Entity
@Table(name = "properties")
public class Property extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY) // AUTO_INCREMENT
	@Column(name = "property_id")
	private Long id;

	@Column(name = "property_name", nullable = false, length = 50)
	private String propertyName;

	/** 소개 문구. 리치텍스트 HTML 을 그대로 저장합니다. */
	@Column(name = "description", nullable = false, columnDefinition = "TEXT")
	private String description;

	@Enumerated(EnumType.STRING)
	@ColumnDefault("'DRAFT'") // 정의서의 기본값 "DRAFT". 문자열이라 작은따옴표로 한 번 더 감쌉니다.
	@Column(name = "status", nullable = false, columnDefinition = "VARCHAR(20)")
	private PropertyStatus status;

	/**
	 * 최종 수정자 ID. users 테이블(role=HOST)을 가리킵니다.
	 * {@code @ManyToOne} 을 쓰지 않고 값(Long)만 들고 있습니다.
	 * NULL 허용 컬럼이며, 현재는 채우는 곳이 없어 NULL 로 저장됩니다.
	 */
	@Column(name = "updated_by")
	private Long updatedBy;

	/** 새 숙소를 만듭니다. 상태는 항상 DRAFT 로 시작합니다. */
	public static Property create(
			String propertyName,
			String description
	) {
		Property property = new Property();
		property.propertyName = propertyName;
		property.description = description;
		property.status = PropertyStatus.DRAFT;
		return property;
	}

	/** 내용을 고칩니다. 게시 상태(DRAFT·PUBLISHED)도 여기서 바꿉니다. */
	public void update(
			String propertyName,
			String description,
			PropertyStatus status
	) {
		this.propertyName = propertyName;
		this.description = description;
		this.status = status;
	}
}
