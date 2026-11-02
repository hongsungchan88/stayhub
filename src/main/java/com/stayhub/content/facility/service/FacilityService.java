package com.stayhub.content.facility.service;

import com.stayhub.common.BusinessException;
import com.stayhub.common.PageResponse;
import com.stayhub.content.facility.dto.FacilityResponse;
import com.stayhub.content.facility.entity.Facility;
import com.stayhub.content.facility.repository.FacilityRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 시설 코드 업무 로직. 조회만 있습니다(등록·수정 API 없음 — 총괄 결정, 데이터는 data.sql).
 */
@Service
@Transactional(readOnly = true)
public class FacilityService {

	/** 목록 정렬: sort_order 오름차순 → 같으면 facility_code 오름차순. */
	private static final Sort LIST_SORT = Sort.by(
			Sort.Order.asc("sortOrder"),
			Sort.Order.asc("id")
	);

	private final FacilityRepository facilityRepository;

	public FacilityService(FacilityRepository facilityRepository) {
		this.facilityRepository = facilityRepository;
	}

	/**
	 * 체크박스 목록. 사용 중(use_yn = true)인 것만, 한 페이지 20건.
	 *
	 * @param page 0부터 시작
	 */
	public PageResponse<FacilityResponse> getList(int page) {
		Pageable pageable = PageRequest.of(page, PageResponse.DEFAULT_PAGE_SIZE, LIST_SORT);
		return PageResponse.from(facilityRepository.findByUseYn(true, pageable).map(FacilityResponse::from));
	}

	/** 상세. 없는 코드면 BusinessException. */
	public FacilityResponse getDetail(String id) {
		return FacilityResponse.from(findOrThrow(id));
	}

	/** 항목 하나를 찾습니다. 없으면 예외. */
	private Facility findOrThrow(String id) {
		return facilityRepository.findById(id)
				.orElseThrow(() -> new BusinessException("시설을 찾을 수 없습니다."));
	}
}
