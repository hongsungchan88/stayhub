package com.stayhub.content.service;

import com.stayhub.common.BusinessException;
import com.stayhub.common.PropertyScope;
import com.stayhub.content.dto.PublicGuidebookResponse;
import com.stayhub.content.dto.PublicPropertyResponse;
import com.stayhub.content.entity.GuidebookCategory;
import com.stayhub.content.facility.dto.FacilityResponse;
import com.stayhub.content.facility.service.FacilityService;
import com.stayhub.content.property.dto.PropertyImageResponse;
import com.stayhub.content.property.entity.Property;
import com.stayhub.content.property.entity.PropertyStatus;
import com.stayhub.content.property.repository.PropertyRepository;
import com.stayhub.content.property.service.PropertyFacilityService;
import com.stayhub.content.property.service.PropertyImageService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게스트(비로그인) 화면용 조회 전용 로직. SCR-GST-01 숙소 소개, SCR-GST-02 가이드북 열람.
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>기존 관리자용 서비스의 조회를 그대로 불러, 게스트에게 보여 줄 값만 골라 담습니다.
 * 새 규칙은 하나뿐입니다: 숙소가 PUBLISHED 가 아니면(DRAFT 거나 없으면) "준비 중인 숙소입니다." ({@link #requirePublished})
 * 쓰기 메서드는 없습니다. 만들지 마세요 — 이 서비스를 부르는 /api/public/** 는 로그인 없이 열려 있습니다.
 */
@Service
@Transactional(readOnly = true)
public class PublicContentService {

	public static final String NOT_READY_MESSAGE = "준비 중인 숙소입니다.";

	private final PropertyRepository propertyRepository;
	private final PropertyFacilityService propertyFacilityService;
	private final FacilityService facilityService;
	private final PropertyImageService propertyImageService;
	private final GuidebookService guidebookService;

	public PublicContentService(
			PropertyRepository propertyRepository,
			PropertyFacilityService propertyFacilityService,
			FacilityService facilityService,
			PropertyImageService propertyImageService,
			GuidebookService guidebookService
	) {
		this.propertyRepository = propertyRepository;
		this.propertyFacilityService = propertyFacilityService;
		this.facilityService = facilityService;
		this.propertyImageService = propertyImageService;
		this.guidebookService = guidebookService;
	}

	/**
	 * 숙소가 PUBLISHED 인지 확인합니다. 아니면(DRAFT 거나 없으면) BusinessException(준비 중) → 400.
	 * 게스트 경로의 공개 여부 규칙은 여기 한 곳뿐입니다. 숙소 소개와 게스트 예약(PublicReservationService)이 부릅니다.
	 */
	public Property requirePublished() {
		return propertyRepository.findById(PropertyScope.DEFAULT_PROPERTY_ID)
				.filter(p -> p.getStatus() == PropertyStatus.PUBLISHED)
				.orElseThrow(() -> new BusinessException(NOT_READY_MESSAGE));
	}

	/** 숙소 소개. PUBLISHED 가 아니면 BusinessException(준비 중). */
	public PublicPropertyResponse getProperty() {
		Property property = requirePublished();

		// 시설: 숙소에 체크된 코드만, 시설 목록의 sort_order 순 (use_yn = false 인 시설은 목록에 없어 빠집니다)
		// shortcut: 시설 목록·사진 목록 모두 첫 페이지(20건)만 봅니다. 사진은 정책상 최대 10장이고 시설은 3종이라 충분. 20건을 넘기면 페이지를 이어 부르기
		List<String> checkedCodes = propertyFacilityService.getCodes();
		List<String> facilities = facilityService.getList(0).content().stream()
				.filter(facility -> checkedCodes.contains(facility.id()))
				.map(FacilityResponse::facilityName)
				.toList();
		List<String> images = propertyImageService.getList(0).content().stream()
				.map(PropertyImageResponse::filePath)
				.toList();

		return new PublicPropertyResponse(property.getPropertyName(), property.getDescription(), facilities, images);
	}

	/**
	 * 가이드북. ACTIVE 만, sort_order 순 (기존 {@link GuidebookService#getList} 그대로).
	 *
	 * @param category 비어 있으면 전체
	 */
	public List<PublicGuidebookResponse> getGuidebooks(GuidebookCategory category) {
		// shortcut: 첫 페이지(20건)만 보여 줍니다(총괄 결정). 한 카테고리·전체가 20건을 넘기 시작하면 페이지를 이어 부르기
		return guidebookService.getList(category, 0).content().stream()
				.map(PublicGuidebookResponse::from)
				.toList();
	}
}
