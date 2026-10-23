package com.stayhub.content.property.service;

import com.stayhub.common.BusinessException;
import com.stayhub.common.PageResponse;
import com.stayhub.content.property.dto.PropertyCreateRequest;
import com.stayhub.content.property.dto.PropertyResponse;
import com.stayhub.content.property.dto.PropertyUpdateRequest;
import com.stayhub.content.property.entity.Property;
import com.stayhub.content.property.repository.PropertyRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 숙소 업무 로직.
 *
 * <p>클래스에 {@code @Transactional(readOnly = true)}, 쓰기 메서드(등록·수정)에만 {@code @Transactional}.
 *
 * <p>유형 ②라 조회에 status 조건을 넣지 않고, 삭제 메서드가 없습니다.
 * {@code property_id} 는 자기 PK 라서 {@code PropertyScope} 값을 넣지 않습니다.
 */
@Service
@Transactional(readOnly = true)
public class PropertyService {

	/** 목록 정렬: property_id 오름차순(등록 순). */
	private static final Sort LIST_SORT = Sort.by(
			Sort.Order.asc("id")
	);

	private final PropertyRepository propertyRepository;

	public PropertyService(PropertyRepository propertyRepository) {
		this.propertyRepository = propertyRepository;
	}

	/**
	 * 목록. 한 페이지 20건.
	 *
	 * @param page 0부터 시작
	 */
	public PageResponse<PropertyResponse> getList(int page) {
		Pageable pageable = PageRequest.of(page, PageResponse.DEFAULT_PAGE_SIZE, LIST_SORT);
		return PageResponse.from(propertyRepository.findAll(pageable).map(PropertyResponse::from));
	}

	/** 상세. 없는 항목이면 BusinessException. */
	public PropertyResponse getDetail(Long id) {
		return PropertyResponse.from(findOrThrow(id));
	}

	/** 등록. 상태는 DRAFT 로 시작합니다. */
	@Transactional
	public PropertyResponse create(PropertyCreateRequest request) {
		Property property = Property.create(
				request.propertyName(),
				request.description()
		);
		return PropertyResponse.from(propertyRepository.save(property));
	}

	/** 수정. 게시 상태도 여기서 바꿉니다. 없는 항목이면 BusinessException. */
	@Transactional
	public PropertyResponse update(Long id, PropertyUpdateRequest request) {
		Property property = findOrThrow(id);
		property.update(
				request.propertyName(),
				request.description(),
				request.status()
		);
		// 수정일시(updatedAt)는 반영되는 순간에 채워지므로, 응답에 새 값을 담으려고 먼저 반영합니다.
		propertyRepository.flush();
		return PropertyResponse.from(property);
	}

	/** 항목 하나를 찾습니다. 없으면 예외. */
	private Property findOrThrow(Long id) {
		return propertyRepository.findById(id)
				.orElseThrow(() -> new BusinessException("숙소를 찾을 수 없습니다."));
	}
}
