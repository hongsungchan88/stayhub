package com.stayhub.content.property.repository;

import com.stayhub.content.property.entity.PropertyFacility;
import com.stayhub.content.property.entity.PropertyFacilityId;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 숙소-시설 매핑 DB 접근.
 *
 * <p>복합 PK 라서 제네릭 두 번째 자리가 {@link PropertyFacilityId} 입니다.
 * property_id 는 조회 조건에 넣지 않습니다(정책 32행) — 기본 메서드 {@code findAll}·{@code saveAll}·{@code deleteAll} 만 씁니다.
 */
public interface PropertyFacilityRepository extends JpaRepository<PropertyFacility, PropertyFacilityId> {
}
