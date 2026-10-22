package com.stayhub.content.property.entity;

import com.stayhub.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

/**
 * 숙소 이미지 (테이블 {@code property_images}). 유형 ③ — status 가 없고 삭제 기능도 없습니다.
 *
 * <p>소개 페이지 대표 사진(1~10장)의 저장 경로·용량·노출 순서를 관리합니다.
 *
 * <p>값을 바꾸는 방법은 {@link #create} / {@link #update} 뿐입니다. (삭제 없음)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 가 쓰는 빈 생성자. 직접 new 하지 말고 create() 를 쓰세요.
@Entity
@Table(name = "property_images")
public class PropertyImage extends BaseEntity {

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

	/** 파일 용량(byte). 10MB 초과는 요청 단계에서 거부합니다. */
	@Column(name = "file_size", nullable = false)
	private int fileSize;

	/** 파일 확장자. jpg, png 만 허용합니다. */
	@Column(name = "file_ext", nullable = false, length = 10)
	private String fileExt;

	/** 대표 사진 여부. CMS-006(v2 이관) 용으로 미리 둡니다. */
	@ColumnDefault("false") // 정의서의 "기본 FALSE"
	@Column(name = "is_main", nullable = false)
	private boolean isMain;

	@ColumnDefault("0") // 정의서의 "기본 0"
	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	/**
	 * 새 숙소 이미지를 만듭니다.
	 *
	 * @param isMain    비어 있으면 false
	 * @param sortOrder 비어 있으면 0. 0끼리는 등록 순(image_id 순)으로 정렬됩니다.
	 */
	public static PropertyImage create(
			Long propertyId,
			String filePath,
			Integer fileSize,
			String fileExt,
			Boolean isMain,
			Integer sortOrder
	) {
		PropertyImage image = new PropertyImage();
		image.propertyId = propertyId;
		image.filePath = filePath;
		image.fileSize = fileSize;
		image.fileExt = fileExt;
		image.isMain = defaultIsMain(isMain);
		image.sortOrder = defaultSortOrder(sortOrder);
		return image;
	}

	/** 내용을 고칩니다. propertyId 는 여기서 바꾸지 않습니다. */
	public void update(
			String filePath,
			Integer fileSize,
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
