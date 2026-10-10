package com.stayhub.content.property.service;

import com.stayhub.common.BusinessException;
import com.stayhub.common.PageResponse;
import com.stayhub.common.PropertyScope;
import com.stayhub.content.property.dto.PropertyImageCreateRequest;
import com.stayhub.content.property.dto.PropertyImageResponse;
import com.stayhub.content.property.dto.PropertyImageUpdateRequest;
import com.stayhub.content.property.entity.PropertyImage;
import com.stayhub.content.property.repository.PropertyImageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 숙소 이미지 업무 로직.
 *
 * <p>컨트롤러에게 요청 DTO 를 받아 엔티티를 만들거나 고치고, 결과를 응답 DTO 로 바꿔 돌려줍니다.
 * 엔티티는 이 클래스 밖으로 나가지 않습니다.
 *
 * <p>유형 ③(status 없음)이라 조회에 status 조건이 없고, 삭제 기능이 없습니다.
 *
 * <p><b>숙소 ID</b> — {@code property_id} 는 등록할 때만 {@link PropertyScope} 값을 넣고,
 * 조회 조건에는 넣지 않습니다(정책정의서 32행).
 */
@Service
@Transactional(readOnly = true)
public class PropertyImageService {

	/** 목록 정렬: sort_order 오름차순 → 같으면 image_id 오름차순(등록 순). */
	private static final Sort LIST_SORT = Sort.by(
			Sort.Order.asc("sortOrder"),
			Sort.Order.asc("id")
	);

	private final PropertyImageRepository propertyImageRepository;

	public PropertyImageService(PropertyImageRepository propertyImageRepository) {
		this.propertyImageRepository = propertyImageRepository;
	}

	/**
	 * 목록. 한 페이지 20건.
	 *
	 * @param page 0부터 시작
	 */
	public PageResponse<PropertyImageResponse> getList(int page) {
		Pageable pageable = PageRequest.of(page, PageResponse.DEFAULT_PAGE_SIZE, LIST_SORT);

		Page<PropertyImage> propertyImages = propertyImageRepository.findAll(pageable);

		return PageResponse.from(propertyImages.map(PropertyImageResponse::from));
	}

	/** 상세. 없는 항목이면 BusinessException. */
	public PropertyImageResponse getDetail(Long id) {
		return PropertyImageResponse.from(findOrThrow(id));
	}

	/** 등록. property_id 는 요청에서 받지 않고 여기서 상수로 넣습니다. */
	@Transactional
	public PropertyImageResponse create(PropertyImageCreateRequest request) {
		PropertyImage propertyImage = PropertyImage.create(
				PropertyScope.DEFAULT_PROPERTY_ID,
				request.filePath(),
				request.fileSize(),
				request.fileExt(),
				request.isMain(),
				request.sortOrder()
		);
		return PropertyImageResponse.from(propertyImageRepository.save(propertyImage));
	}

	/** 수정. 없는 항목이면 BusinessException. */
	@Transactional
	public PropertyImageResponse update(Long id, PropertyImageUpdateRequest request) {
		PropertyImage propertyImage = findOrThrow(id);
		propertyImage.update(
				request.filePath(),
				request.fileSize(),
				request.fileExt(),
				request.isMain(),
				request.sortOrder()
		);
		// save() 를 부르지 않아도 트랜잭션이 끝날 때 바뀐 값이 자동으로 DB 에 반영됩니다. (변경 감지)
		// 이 테이블은 updated_at 이 없어 응답에 담을 새 수정일시가 없으므로 flush() 를 부르지 않습니다.
		return PropertyImageResponse.from(propertyImage);
	}

	/** 항목 하나를 찾습니다. 없으면 예외. */
	private PropertyImage findOrThrow(Long id) {
		return propertyImageRepository.findById(id)
				.orElseThrow(() -> new BusinessException("숙소 이미지를 찾을 수 없습니다."));
	}
}
