package com.stayhub.content.property.service;

import com.stayhub.common.BusinessException;
import com.stayhub.common.PageResponse;
import com.stayhub.common.PropertyScope;
import com.stayhub.content.property.dto.PropertyImageCreateRequest;
import com.stayhub.content.property.dto.PropertyImageResponse;
import com.stayhub.content.property.dto.PropertyImageUpdateRequest;
import com.stayhub.content.property.entity.PropertyImage;
import com.stayhub.content.property.entity.PropertyStatus;
import com.stayhub.content.property.repository.PropertyImageRepository;
import com.stayhub.content.property.repository.PropertyRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 숙소 이미지 업무 로직.
 *
 * <p>클래스에 {@code @Transactional(readOnly = true)}, 쓰기 메서드(등록·수정·삭제)에만 {@code @Transactional}.
 *
 * <p>유형 ③이라 status 조건이 없습니다.
 * {@code property_id} 는 등록할 때만 {@link PropertyScope} 값을 넣고, 조회 조건에는 넣지 않습니다(정책정의서 32행).
 *
 * <p><b>장수 규칙</b> (정의서 "대표 사진 1~10장", 총괄 결정 2026-11-25)
 * <ul>
 *   <li>최대 10장 — 등록할 때 막습니다</li>
 *   <li>최소 1장 — 숙소를 게시(PUBLISHED)로 바꿀 때 검사합니다({@code PropertyService.update}).
 *       그래서 게시 중에는 마지막 1장을 지울 수 없습니다</li>
 * </ul>
 * 숙소가 1번 하나뿐이라 장수는 테이블 전체 {@code count()} 로 셉니다.
 */
@Service
@Transactional(readOnly = true)
public class PropertyImageService {

	/** 정의서 "이미지 업로드 최대 10장". */
	public static final int MAX_IMAGES = 10;

	public static final String TOO_MANY_IMAGES = "사진은 최대 " + MAX_IMAGES + "장까지 등록할 수 있습니다.";
	public static final String LAST_IMAGE_PUBLISHED = "게시 중에는 마지막 사진을 삭제할 수 없습니다. 게시 상태를 바꾸거나 다른 사진을 먼저 등록해 주세요.";

	/** 목록 정렬: sort_order 오름차순 → 같으면 image_id 오름차순(등록 순). */
	private static final Sort LIST_SORT = Sort.by(
			Sort.Order.asc("sortOrder"),
			Sort.Order.asc("id")
	);

	private final PropertyImageRepository propertyImageRepository;
	private final PropertyRepository propertyRepository;

	public PropertyImageService(
			PropertyImageRepository propertyImageRepository,
			PropertyRepository propertyRepository
	) {
		this.propertyImageRepository = propertyImageRepository;
		this.propertyRepository = propertyRepository;
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

	/** 등록. property_id 는 요청에서 받지 않고 여기서 상수로 넣습니다. 이미 10장이면 BusinessException. */
	@Transactional
	public PropertyImageResponse create(PropertyImageCreateRequest request) {
		requireRoomForOneMore();
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

	/**
	 * 삭제. 행을 실제로 지웁니다(정책정의서 24행 예외, 총괄 결정).
	 * 없는 항목이거나, 숙소가 게시 중인데 마지막 1장이면 BusinessException.
	 *
	 * @return 지운 사진의 file_path. 저장된 파일은 컨트롤러가 이 값으로 지웁니다(파일 삭제가 실패해도 행 삭제는 그대로)
	 */
	@Transactional
	public String delete(Long id) {
		PropertyImage image = findOrThrow(id);
		if (isPublished() && propertyImageRepository.count() <= 1) {
			throw new BusinessException(LAST_IMAGE_PUBLISHED);
		}
		propertyImageRepository.delete(image);
		return image.getFilePath();
	}

	/** 10장이 이미 있으면 예외. 업로드(파일 저장 전)에서도 먼저 부릅니다. */
	public void requireRoomForOneMore() {
		if (propertyImageRepository.count() >= MAX_IMAGES) {
			throw new BusinessException(TOO_MANY_IMAGES);
		}
	}

	/** 편집 대상 숙소(1번)가 게시 중인지. 행이 없으면 게시 중이 아닌 것으로 봅니다. */
	private boolean isPublished() {
		return propertyRepository.findById(PropertyScope.DEFAULT_PROPERTY_ID)
				.map(property -> property.getStatus() == PropertyStatus.PUBLISHED)
				.orElse(false);
	}

	/** 항목 하나를 찾습니다. 없으면 예외. */
	private PropertyImage findOrThrow(Long id) {
		return propertyImageRepository.findById(id)
				.orElseThrow(() -> new BusinessException("숙소 이미지를 찾을 수 없습니다."));
	}
}
