package com.stayhub.content.property.service;

import com.stayhub.common.BusinessException;
import com.stayhub.common.PropertyScope;
import com.stayhub.content.facility.entity.Facility;
import com.stayhub.content.facility.repository.FacilityRepository;
import com.stayhub.content.property.entity.PropertyFacility;
import com.stayhub.content.property.repository.PropertyFacilityRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 숙소-시설 매핑 업무 로직.
 *
 * <p>숙소 소개를 저장할 때 {@link PropertyService#update} 가 {@link #sync} 를 부릅니다.
 * 같은 트랜잭션이라, 시설 코드가 잘못되면 숙소 소개 수정도 함께 취소됩니다.
 */
@Service
@Transactional(readOnly = true)
public class PropertyFacilityService {

	private final PropertyFacilityRepository propertyFacilityRepository;
	private final FacilityRepository facilityRepository;

	public PropertyFacilityService(
			PropertyFacilityRepository propertyFacilityRepository,
			FacilityRepository facilityRepository
	) {
		this.propertyFacilityRepository = propertyFacilityRepository;
		this.facilityRepository = facilityRepository;
	}

	/** 숙소에 체크된 시설 코드 목록 (코드 순). */
	public List<String> getCodes() {
		return propertyFacilityRepository.findAll(Sort.by(Sort.Order.asc("facilityCode"))).stream()
				.map(PropertyFacility::getFacilityCode)
				.toList();
	}

	/**
	 * 요청 목록과 DB 의 기존 목록을 비교해서 <b>바뀐 것만</b> 반영합니다. (총괄 결정)
	 * <ul>
	 *   <li>요청에서 빠진 코드 → 행 삭제</li>
	 *   <li>요청에 새로 생긴 코드 → 행 추가</li>
	 *   <li>그대로인 코드 → 건드리지 않음</li>
	 * </ul>
	 * 전부 지우고 다시 넣지 않는 이유: 같은 트랜잭션에서 JPA 가 INSERT 를 DELETE 보다 먼저 실행해서
	 * 그대로인 코드가 PK 중복 오류를 냅니다.
	 *
	 * @param codes 체크된 시설 코드. 비어 있으면 전부 해제. 같은 코드가 여러 번 와도 한 번으로 봅니다.
	 * @throws BusinessException facilities 에 없거나 use_yn = false 인 코드가 있으면 (아무것도 반영하지 않음)
	 */
	@Transactional
	public void sync(List<String> codes) {
		Set<String> requested = new LinkedHashSet<>(codes);
		validateUsable(requested);

		List<PropertyFacility> current = propertyFacilityRepository.findAll();
		Set<String> currentCodes = current.stream()
				.map(PropertyFacility::getFacilityCode)
				.collect(Collectors.toSet());

		// 빠진 코드 → 삭제
		List<PropertyFacility> toDelete = current.stream()
				.filter(propertyFacility -> !requested.contains(propertyFacility.getFacilityCode()))
				.toList();
		propertyFacilityRepository.deleteAll(toDelete);

		// 새로 생긴 코드 → 추가
		List<PropertyFacility> toAdd = requested.stream()
				.filter(code -> !currentCodes.contains(code))
				.map(code -> PropertyFacility.create(PropertyScope.DEFAULT_PROPERTY_ID, code))
				.toList();
		propertyFacilityRepository.saveAll(toAdd);
	}

	/** 요청 코드가 모두 facilities 에 있고 사용 중(use_yn = true)인지 확인합니다. */
	private void validateUsable(Set<String> requested) {
		Set<String> usableCodes = facilityRepository.findAllById(requested).stream()
				.filter(Facility::isUseYn)
				.map(Facility::getId)
				.collect(Collectors.toSet());

		for (String code : requested) {
			if (!usableCodes.contains(code)) {
				throw new BusinessException("사용할 수 없는 시설 코드입니다: " + code);
			}
		}
	}
}
