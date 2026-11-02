package com.stayhub.content.property;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stayhub.common.BusinessException;
import com.stayhub.common.PropertyScope;
import com.stayhub.content.facility.entity.Facility;
import com.stayhub.content.facility.repository.FacilityRepository;
import com.stayhub.content.property.entity.PropertyFacility;
import com.stayhub.content.property.repository.PropertyFacilityRepository;
import com.stayhub.content.property.service.PropertyFacilityService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 숙소-시설 매핑 서비스 테스트.
 *
 * <p>{@code @Transactional} 이라 테스트가 끝나면 넣었던 데이터가 전부 되돌려집니다(롤백).
 * 매번 {@code flush()} 를 불러 SQL 을 실제로 DB 에 보내서, PK 중복 같은 오류가 테스트 안에서 드러나게 합니다.
 */
@SpringBootTest
@Transactional
class PropertyFacilityServiceTest {

	@Autowired
	private PropertyFacilityService propertyFacilityService;

	@Autowired
	private PropertyFacilityRepository propertyFacilityRepository;

	@Autowired
	private FacilityRepository facilityRepository;

	@BeforeEach
	void setUp() {
		// 경고: @Transactional을 지우면 개발 DB의 이 테이블들이 실제로 비워집니다. 지우지 마세요.
		propertyFacilityRepository.deleteAll();
		facilityRepository.deleteAll();
		// 지운 것을 먼저 DB 에 반영합니다. 안 하면 data.sql 로 들어간 PARKING 등과 PK 가 겹쳐 오류가 납니다.
		// (JPA 는 INSERT 를 DELETE 보다 먼저 실행합니다)
		facilityRepository.flush();
		facilityRepository.save(Facility.create("PARKING", "주차", 1, true));
		facilityRepository.save(Facility.create("WIFI", "와이파이", 2, true));
		facilityRepository.save(Facility.create("BREAKFAST", "조식", 3, true));
		facilityRepository.save(Facility.create("POOL", "수영장", 4, false)); // 사용 안 함
		facilityRepository.flush();
	}

	@Test
	@DisplayName("처음 저장하면 체크한 시설이 들어가고, property_id 에는 PropertyScope 값이 들어간다")
	void firstSaveInserts() {
		propertyFacilityService.sync(List.of("PARKING", "WIFI"));
		propertyFacilityRepository.flush();

		assertThat(propertyFacilityService.getCodes()).containsExactly("PARKING", "WIFI");
		assertThat(propertyFacilityRepository.findAll())
				.extracting(PropertyFacility::getPropertyId)
				.containsOnly(PropertyScope.DEFAULT_PROPERTY_ID);
	}

	@Test
	@DisplayName("다시 저장하면 바뀐 것만 반영된다 — 빠진 코드는 삭제, 새 코드는 추가, 그대로인 코드는 PK 오류 없이 유지")
	void secondSaveAppliesOnlyChanges() {
		propertyFacilityService.sync(List.of("PARKING", "WIFI"));
		propertyFacilityRepository.flush();

		// PARKING 해제, WIFI 유지, BREAKFAST 추가
		propertyFacilityService.sync(List.of("WIFI", "BREAKFAST"));
		propertyFacilityRepository.flush();
		assertThat(propertyFacilityService.getCodes()).containsExactly("BREAKFAST", "WIFI");

		// 같은 목록을 한 번 더 저장해도 오류 없이 그대로
		propertyFacilityService.sync(List.of("WIFI", "BREAKFAST"));
		propertyFacilityRepository.flush();
		assertThat(propertyFacilityService.getCodes()).containsExactly("BREAKFAST", "WIFI");

		// 빈 목록이면 전부 해제
		propertyFacilityService.sync(List.of());
		propertyFacilityRepository.flush();
		assertThat(propertyFacilityService.getCodes()).isEmpty();
	}

	@Test
	@DisplayName("없는 코드나 사용 안 하는 코드가 있으면 BusinessException 이 나고, 아무것도 반영되지 않는다")
	void invalidCodeThrows() {
		propertyFacilityService.sync(List.of("PARKING"));
		propertyFacilityRepository.flush();

		assertThatThrownBy(() -> propertyFacilityService.sync(List.of("WIFI", "NOT_EXIST")))
				.isInstanceOf(BusinessException.class)
				.hasMessage("사용할 수 없는 시설 코드입니다: NOT_EXIST");

		assertThatThrownBy(() -> propertyFacilityService.sync(List.of("POOL")))
				.isInstanceOf(BusinessException.class)
				.hasMessage("사용할 수 없는 시설 코드입니다: POOL");

		// 검사가 저장보다 먼저라서 기존 값은 그대로
		assertThat(propertyFacilityService.getCodes()).containsExactly("PARKING");
	}
}
