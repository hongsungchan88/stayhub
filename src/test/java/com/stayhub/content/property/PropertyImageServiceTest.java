package com.stayhub.content.property;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stayhub.common.BusinessException;
import com.stayhub.common.PageResponse;
import com.stayhub.common.PropertyScope;
import com.stayhub.content.property.dto.PropertyImageCreateRequest;
import com.stayhub.content.property.dto.PropertyImageResponse;
import com.stayhub.content.property.dto.PropertyImageUpdateRequest;
import com.stayhub.content.property.repository.PropertyImageRepository;
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
 * <p>실제 DB(application-local.yml 의 MySQL)에 붙어서 등록·조회·수정이 제대로 되는지 확인합니다.
 * {@code @Transactional} 이라 테스트가 끝나면 넣었던 데이터가 전부 되돌려집니다(롤백).
 *
 * <p>유형 ③이라 삭제 테스트는 없고, 없는 항목 조회는 99999L 로 확인합니다.
 */
@SpringBootTest
@Transactional
class PropertyImageServiceTest {

	@Autowired
	private PropertyImageService propertyImageService;

	@Autowired
	private PropertyImageRepository propertyImageRepository;

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

	/** 테스트용 등록 요청. 용량·확장자는 테스트마다 중요하지 않아 고정값으로 둡니다. */
	private PropertyImageCreateRequest createRequest(String filePath) {
		return new PropertyImageCreateRequest(filePath, 1024, "jpg", null, null);
	}
}
