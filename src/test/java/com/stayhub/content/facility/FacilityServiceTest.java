package com.stayhub.content.facility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stayhub.common.BusinessException;
import com.stayhub.common.PageResponse;
import com.stayhub.content.facility.dto.FacilityResponse;
import com.stayhub.content.facility.entity.Facility;
import com.stayhub.content.facility.repository.FacilityRepository;
import com.stayhub.content.facility.service.FacilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 시설 코드 서비스 테스트.
 *
 * <p>{@code @Transactional} 이라 테스트가 끝나면 넣었던 데이터가 전부 되돌려집니다(롤백).
 *
 * <p>등록·수정 API 가 없어서 가이드북의 "등록→목록, 수정" 대신 "목록(use_yn 필터·정렬), 상세" 를 확인합니다.
 * PK 가 문자 코드라서 없는 항목 조회는 99999L 대신 "NOT_EXIST" 로 확인합니다.
 */
@SpringBootTest
@Transactional
class FacilityServiceTest {

	@Autowired
	private FacilityService facilityService;

	@Autowired
	private FacilityRepository facilityRepository;

	@BeforeEach
	void setUp() {
		// data.sql 로 들어간 데이터 때문에 결과가 흔들리지 않도록 테스트 안에서만 비우고 새로 넣습니다.
		// 경고: @Transactional을 지우면 개발 DB의 이 테이블이 실제로 비워집니다. 지우지 마세요.
		facilityRepository.deleteAll();
		// 지운 것을 먼저 DB 에 반영합니다. 안 하면 data.sql 로 들어간 PARKING 등과 PK 가 겹쳐 오류가 납니다.
		// (JPA 는 INSERT 를 DELETE 보다 먼저 실행합니다)
		facilityRepository.flush();
		facilityRepository.save(Facility.create("WIFI", "와이파이", 2, true));
		facilityRepository.save(Facility.create("PARKING", "주차", 1, true));
		facilityRepository.save(Facility.create("POOL", "수영장", 3, false));
	}

	@Test
	@DisplayName("목록에는 사용 중인 시설만 sort_order 순으로 나온다")
	void listShowsOnlyUsableInOrder() {
		PageResponse<FacilityResponse> list = facilityService.getList(0);

		assertThat(list.content()).extracting(FacilityResponse::id).containsExactly("PARKING", "WIFI");
	}

	@Test
	@DisplayName("상세 조회하면 시설 정보가 나온다")
	void getDetailReturnsFacility() {
		FacilityResponse found = facilityService.getDetail("PARKING");

		assertThat(found.facilityName()).isEqualTo("주차");
		assertThat(found.sortOrder()).isEqualTo(1);
		assertThat(found.useYn()).isTrue();
	}

	@Test
	@DisplayName("없는 항목을 상세 조회하면 BusinessException 이 난다")
	void getMissingThrows() {
		assertThatThrownBy(() -> facilityService.getDetail("NOT_EXIST"))
				.isInstanceOf(BusinessException.class)
				.hasMessage("시설을 찾을 수 없습니다.");
	}
}
