package com.stayhub.content.facility.repository;

import com.stayhub.content.facility.entity.Facility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 시설 코드 DB 접근.
 *
 * <p>PK 가 문자 코드라서 제네릭이 {@code JpaRepository<Facility, String>} 입니다. (가이드북은 Long)
 * 유형 ③이라 status 조건이 없습니다. 상세·여러 건 조회는 기본 메서드 {@code findById}·{@code findAllById} 를 씁니다.
 */
public interface FacilityRepository extends JpaRepository<Facility, String> {

	/** 체크박스 목록 — 사용 중(use_yn = true)인 것만. */
	Page<Facility> findByUseYn(boolean useYn, Pageable pageable);
}
