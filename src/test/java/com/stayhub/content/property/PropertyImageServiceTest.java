package com.stayhub.content.property;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stayhub.common.BusinessException;
import com.stayhub.common.PageResponse;
import com.stayhub.common.PropertyScope;
import com.stayhub.content.property.dto.PropertyImageCreateRequest;
import com.stayhub.content.property.dto.PropertyImageResponse;
import com.stayhub.content.property.dto.PropertyImageUpdateRequest;
import com.stayhub.content.property.entity.Property;
import com.stayhub.content.property.entity.PropertyStatus;
import com.stayhub.content.property.repository.PropertyImageRepository;
import com.stayhub.content.property.repository.PropertyRepository;
import com.stayhub.content.property.service.PropertyImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 숙소 이미지 서비스 테스트.
 *
 * <p>실제 DB(application-local.yml 의 MySQL)에 붙어서 등록·조회·수정·삭제가 제대로 되는지 확인합니다.
 * {@code @Transactional} 이라 테스트가 끝나면 넣었던 데이터가 전부 되돌려집니다(롤백).
 *
 * <p>삭제는 행을 실제로 지웁니다(정책정의서 24행 예외, 총괄 결정 2026-11-25).
 * 장수 규칙: 최대 10장, 게시 중 마지막 1장 삭제 불가. 게시 상태는 data.sql 이 넣는 1번 숙소로 확인합니다.
 */
@SpringBootTest
@Transactional
class PropertyImageServiceTest {

	@Autowired
	private PropertyImageService propertyImageService;

	@Autowired
	private PropertyImageRepository propertyImageRepository;

	@Autowired
	private PropertyRepository propertyRepository;

	@BeforeEach
	void setUp() {
		// 개발 DB 에 이미 들어 있는 데이터 때문에 결과가 흔들리지 않도록 테스트 안에서만 비웁니다.
		// 경고: @Transactional을 지우면 개발 DB의 이 테이블이 실제로 비워집니다. 지우지 마세요.
		propertyImageRepository.deleteAll();
	}

	@Test
	@DisplayName("등록하면 목록에 나오고, property_id 에는 PropertyScope 값이 들어간다")
	void createThenAppearsInList() {
		PropertyImageResponse created = propertyImageService.create(createRequest("/upload/property/1.jpg"));

		PageResponse<PropertyImageResponse> all = propertyImageService.getList(0);
		assertThat(all.content()).extracting(PropertyImageResponse::id).containsExactly(created.id());

		// 비워 보낸 선택값은 정의서 기본값으로 저장된다
		assertThat(created.isMain()).isFalse();
		assertThat(created.sortOrder()).isZero();

		assertThat(propertyImageRepository.findById(created.id()))
				.get()
				.extracting(image -> image.getPropertyId())
				.isEqualTo(PropertyScope.DEFAULT_PROPERTY_ID);
	}

	@Test
	@DisplayName("수정하면 바뀐 값이 조회된다")
	void updateIsApplied() {
		PropertyImageResponse created = propertyImageService.create(createRequest("/upload/property/1.jpg"));

		propertyImageService.update(created.id(), new PropertyImageUpdateRequest(
				"/upload/property/2.png", 2048, "png", true, 3));

		PropertyImageResponse found = propertyImageService.getDetail(created.id());
		assertThat(found.filePath()).isEqualTo("/upload/property/2.png");
		assertThat(found.fileSize()).isEqualTo(2048);
		assertThat(found.fileExt()).isEqualTo("png");
		assertThat(found.isMain()).isTrue();
		assertThat(found.sortOrder()).isEqualTo(3);
	}

	@Test
	@DisplayName("없는 항목을 상세 조회하면 BusinessException 이 난다")
	void getMissingThrows() {
		assertThatThrownBy(() -> propertyImageService.getDetail(99999L))
				.isInstanceOf(BusinessException.class)
				.hasMessage("숙소 이미지를 찾을 수 없습니다.");
	}

	@Test
	@DisplayName("10장이 있으면 11번째 등록은 BusinessException 이 나고 10장 그대로다")
	void eleventhImageIsRejected() {
		for (int i = 1; i <= PropertyImageService.MAX_IMAGES; i++) {
			propertyImageService.create(createRequest("/uploads/" + i + ".jpg"));
		}

		assertThatThrownBy(() -> propertyImageService.create(createRequest("/uploads/11.jpg")))
				.isInstanceOf(BusinessException.class)
				.hasMessage(PropertyImageService.TOO_MANY_IMAGES);
		assertThat(propertyImageRepository.count()).isEqualTo(PropertyImageService.MAX_IMAGES);
	}

	@Test
	@DisplayName("삭제하면 행이 실제로 지워지고, 지운 사진의 경로를 돌려준다")
	void deleteRemovesRow() {
		setPropertyStatus(PropertyStatus.DRAFT);
		PropertyImageResponse created = propertyImageService.create(createRequest("/uploads/a.jpg"));

		String deletedPath = propertyImageService.delete(created.id());

		assertThat(deletedPath).isEqualTo("/uploads/a.jpg");
		assertThat(propertyImageRepository.findById(created.id())).isEmpty();
		assertThatThrownBy(() -> propertyImageService.delete(created.id()))
				.isInstanceOf(BusinessException.class)
				.hasMessage("숙소 이미지를 찾을 수 없습니다.");
	}

	@Test
	@DisplayName("게시 중에는 마지막 1장을 지울 수 없고, 2장 이상이면 지울 수 있다")
	void lastImageCannotBeDeletedWhilePublished() {
		setPropertyStatus(PropertyStatus.PUBLISHED);
		PropertyImageResponse first = propertyImageService.create(createRequest("/uploads/a.jpg"));

		assertThatThrownBy(() -> propertyImageService.delete(first.id()))
				.isInstanceOf(BusinessException.class)
				.hasMessage(PropertyImageService.LAST_IMAGE_PUBLISHED);
		assertThat(propertyImageRepository.count()).isEqualTo(1);

		propertyImageService.create(createRequest("/uploads/b.jpg"));
		propertyImageService.delete(first.id());
		assertThat(propertyImageRepository.count()).isEqualTo(1);
	}

	@Test
	@DisplayName("게시 전(DRAFT)에는 마지막 1장도 지울 수 있다")
	void lastImageCanBeDeletedWhileDraft() {
		setPropertyStatus(PropertyStatus.DRAFT);
		PropertyImageResponse only = propertyImageService.create(createRequest("/uploads/a.jpg"));

		propertyImageService.delete(only.id());

		assertThat(propertyImageRepository.count()).isZero();
	}

	/** 편집 대상 1번 숙소(data.sql 이 넣음)의 게시 상태를 바꿉니다. 테스트가 끝나면 롤백됩니다. */
	private void setPropertyStatus(PropertyStatus status) {
		Property property = propertyRepository.findById(PropertyScope.DEFAULT_PROPERTY_ID).orElseThrow();
		property.update(property.getPropertyName(), property.getDescription(), status);
		propertyRepository.flush();
	}

	/** 테스트용 등록 요청. 용량·확장자는 테스트마다 중요하지 않아 고정값으로 둡니다. */
	private PropertyImageCreateRequest createRequest(String filePath) {
		return new PropertyImageCreateRequest(filePath, 1024, "jpg", null, null);
	}
}
