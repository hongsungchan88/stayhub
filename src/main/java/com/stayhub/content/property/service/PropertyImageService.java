package com.stayhub.content.property.service;

import com.stayhub.common.BusinessException;
import com.stayhub.common.PageResponse;
import com.stayhub.common.PropertyScope;
import com.stayhub.content.property.dto.PropertyImageCreateRequest;
import com.stayhub.content.property.dto.PropertyImageResponse;
import com.stayhub.content.property.dto.PropertyImageUpdateRequest;
import com.stayhub.content.property.entity.PropertyImage;
import com.stayhub.content.property.repository.PropertyImageRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 숙소 이미지 업무 로직.
 *
 * <p>클래스에 {@code @Transactional(readOnly = true)}, 쓰기 메서드(등록·수정)에만 {@code @Transactional}.
 *
 * <p>유형 ③이라 status 조건이 없고, 삭제 메서드가 없습니다.
 * {@code property_id} 는 등록할 때만 {@link PropertyScope} 값을 넣고, 조회 조건에는 넣지 않습니다(정책정의서 32행).
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
		return PageResponse.from(propertyImageRepository.findAll(pageable).map(PropertyImageResponse::from));
	}

	/** 상세. 없는 항목이면 BusinessException. */
	public PropertyImageResponse getDetail(Long id) {
		return PropertyImageResponse.from(findOrThrow(id));
	}

	/** 등록. property_id 는 요청에서 받지 않고 여기서 상수로 넣습니다. */
	@Transactional
	public PropertyImageResponse create(PropertyImageCreateRequest request) {
		PropertyImage image = PropertyImage.create(
				PropertyScope.DEFAULT_PROPERTY_ID,
				request.filePath(),
				request.fileSize(),
				request.fileExt(),
				request.isMain(),
				request.sortOrder()
		);
		return PropertyImageResponse.from(propertyImageRepository.save(image));
	}

	/** 수정. 없는 항목이면 BusinessException. */
	@Transactional
	public PropertyImageResponse update(Long id, PropertyImageUpdateRequest request) {
		PropertyImage image = findOrThrow(id);
		image.update(
				request.filePath(),
				request.fileSize(),
				request.fileExt(),
				request.isMain(),
				request.sortOrder()
		);
		propertyImageRepository.flush();
		return PropertyImageResponse.from(image);
	}

	/** 항목 하나를 찾습니다. 없으면 예외. */
	private PropertyImage findOrThrow(Long id) {
		return propertyImageRepository.findById(id)
				.orElseThrow(() -> new BusinessException("숙소 이미지를 찾을 수 없습니다."));
	}
}
