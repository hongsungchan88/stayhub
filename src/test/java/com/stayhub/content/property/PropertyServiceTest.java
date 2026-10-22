package com.stayhub.content.property;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stayhub.common.BusinessException;
import com.stayhub.common.PageResponse;
import com.stayhub.content.property.dto.PropertyCreateRequest;
import com.stayhub.content.property.dto.PropertyResponse;
import com.stayhub.content.property.dto.PropertyUpdateRequest;
import com.stayhub.content.property.entity.PropertyStatus;
import com.stayhub.content.property.repository.PropertyRepository;
import com.stayhub.content.property.service.PropertyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 숙소 서비스 테스트.
 *
 * <p>실제 DB(application-local.yml 의 MySQL)에 붙어서 등록·조회·수정이 제대로 되는지 확인합니다.
 * {@code @Transactional} 이라 테스트가 끝나면 넣었던 데이터가 전부 되돌려집니다(롤백).
 *
 * <p>유형 ②라 삭제 테스트는 없고, 없는 항목 조회는 99999L 로 확인합니다.
 */
@SpringBootTest
@Transactional
class PropertyServiceTest {

	@Autowired
	private PropertyService propertyService;

	@Autowired
	private PropertyRepository propertyRepository;

	@BeforeEach
	void setUp() {
		// 개발 DB 에 이미 들어 있는 데이터 때문에 결과가 흔들리지 않도록 테스트 안에서만 비웁니다.
		// 경고: @Transactional을 지우면 개발 DB의 이 테이블이 실제로 비워집니다. 지우지 마세요.
		propertyRepository.deleteAll();
	}

	@Test
	@DisplayName("등록하면 목록에 나오고, 상태는 DRAFT 로 시작한다")
	void createThenAppearsInList() {
		PropertyResponse created = propertyService.create(createRequest("바다 앞 펜션"));

		PageResponse<PropertyResponse> all = propertyService.getList(0);
		assertThat(all.content()).extracting(PropertyResponse::id).containsExactly(created.id());
		assertThat(created.status()).isEqualTo(PropertyStatus.DRAFT);
	}

	@Test
	@DisplayName("수정하면 바뀐 값과 게시 상태가 조회된다")
	void updateIsApplied() {
		PropertyResponse created = propertyService.create(createRequest("바다 앞 펜션"));

		propertyService.update(created.id(), new PropertyUpdateRequest(
				"산속 펜션", "<p>조용한 산속 숙소입니다.</p>", PropertyStatus.PUBLISHED));

		PropertyResponse found = propertyService.getDetail(created.id());
		assertThat(found.propertyName()).isEqualTo("산속 펜션");
		assertThat(found.description()).isEqualTo("<p>조용한 산속 숙소입니다.</p>");
		assertThat(found.status()).isEqualTo(PropertyStatus.PUBLISHED);
	}

	@Test
	@DisplayName("없는 항목을 상세 조회하면 BusinessException 이 난다")
	void getMissingThrows() {
		assertThatThrownBy(() -> propertyService.getDetail(99999L))
				.isInstanceOf(BusinessException.class)
				.hasMessage("숙소를 찾을 수 없습니다.");
	}

	/** 테스트용 등록 요청. 소개 문구는 테스트마다 중요하지 않아 고정값으로 둡니다. */
	private PropertyCreateRequest createRequest(String propertyName) {
		return new PropertyCreateRequest(propertyName, "<p>테스트 소개 문구</p>");
	}
}
