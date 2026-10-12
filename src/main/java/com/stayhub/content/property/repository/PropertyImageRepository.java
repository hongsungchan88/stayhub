package com.stayhub.content.property.repository;

import com.stayhub.content.property.entity.PropertyImage;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 숙소 이미지 DB 접근.
 *
 * <p>유형 ③(status 없음)이고 필터 조건도 없으므로 {@code JpaRepository} 기본 메서드
 * {@code findAll(pageable)}·{@code findById} 로 충분해 여기에 메서드를 따로 선언하지 않습니다.
 *
 * <p><b>property_id 는 조회 조건에 넣지 않습니다.</b> 등록할 때 {@code PropertyScope} 값을 넣고,
 * 조회 조건에는 넣지 않습니다(정책 32행).
 */
public interface PropertyImageRepository extends JpaRepository<PropertyImage, Long> {
}
