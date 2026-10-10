package com.stayhub.content.property.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

/**
 * 숙소 이미지 (테이블 {@code property_images}).
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>소개 페이지 대표 사진(1~10장)의 저장 경로, 용량, 노출 순서를 한 행씩 표현합니다.
 *
 * <p>값을 바꾸는 방법은 아래 메서드뿐입니다.
 * <ul>
 *   <li>새로 만들 때 — {@link #create}</li>
 *   <li>고칠 때 — {@link #update}</li>
 * </ul>
 * 유형 ③(status 없음)이라 status 필드와 삭제 기능({@code delete()})이 없습니다.
 *
 * <p><b>BaseEntity 를 상속하지 않는 이유</b>
 * <br>정의서에 {@code created_at} 만 있고 {@code updated_at} 이 없습니다.
 * BaseEntity 를 상속하면 정의서에 없는 {@code updated_at} 컬럼이 생기므로, {@code created_at} 만 직접 둡니다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 가 쓰는 빈 생성자. 직접 new 하지 말고 create() 를 쓰세요.
@Entity
@Table(name = "property_images")
public class PropertyImage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY) // AUTO_INCREMENT
	@Column(name = "image_id")
	private Long id;

	/**
	 * 숙소 ID. properties 테이블을 가리킵니다.
	 * {@code @ManyToOne} 을 쓰지 않고 값(Long)만 들고 있습니다.
	 * 등록할 때 {@code PropertyScope} 값을 넣고, 조회 조건에는 넣지 않습니다(정책 32행).
	 */
	@Column(name = "property_id", nullable = false)
	private Long propertyId;

	/** 서버 저장 경로. */
	@Column(name = "file_path", nullable = false, length = 255)
	private String filePath;

	/** 파일 용량(byte). 10MB 초과는 요청 DTO 에서 거부합니다. */
	@Column(name = "file_size", nullable = false)
	private int fileSize;

	/** 파일 확장자. jpg, png 외에는 요청 DTO 에서 거부합니다. */
	@Column(name = "file_ext", nullable = false, length = 10)
	private String fileExt;

	/** 대표 사진 여부. CMS-006: SEO 대표 이미지 미지정 시 자동 사용. */
	@ColumnDefault("false") // 정의서의 "기본 FALSE"
	@Column(name = "is_main", nullable = false, columnDefinition = "BOOLEAN")
	private boolean isMain;

	@ColumnDefault("0") // 정의서의 "기본 0"
	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	/** 업로드 일시. 저장되는 순간 자동으로 채워집니다. */
	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	/**
	 * 새 숙소 이미지를 만듭니다.
	 *
	 * @param isMain    비어 있으면 false
	 * @param sortOrder 비어 있으면 0. 0끼리는 등록 순(image_id 순)으로 정렬됩니다.
	 */
	public static PropertyImage create(
			Long propertyId,
			String filePath,
			int fileSize,
			String fileExt,
			Boolean isMain,
			Integer sortOrder
	) {
		PropertyImage propertyImage = new PropertyImage();
		propertyImage.propertyId = propertyId;
		propertyImage.filePath = filePath;
		propertyImage.fileSize = fileSize;
		propertyImage.fileExt = fileExt;
		propertyImage.isMain = defaultIsMain(isMain);
		propertyImage.sortOrder = defaultSortOrder(sortOrder);
		return propertyImage;
	}

	/** 내용을 고칩니다. propertyId 는 여기서 바꾸지 않습니다. */
	public void update(
			String filePath,
			int fileSize,
			String fileExt,
			Boolean isMain,
			Integer sortOrder
	) {
		this.filePath = filePath;
		this.fileSize = fileSize;
		this.fileExt = fileExt;
		this.isMain = defaultIsMain(isMain);
		this.sortOrder = defaultSortOrder(sortOrder);
	}

	/** 정의서의 "기본 FALSE" — 비어 있으면 false 를 넣습니다. */
	private static boolean defaultIsMain(Boolean isMain) {
		return isMain != null && isMain;
	}

	/** 정의서의 "기본 0" — 비어 있으면 0 을 넣습니다. */
	private static int defaultSortOrder(Integer sortOrder) {
		return sortOrder == null ? 0 : sortOrder;
	}
}
