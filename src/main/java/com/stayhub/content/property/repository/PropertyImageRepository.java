package com.stayhub.content.property.repository;

import com.stayhub.content.property.entity.PropertyImage;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 숙소 이미지 DB 접근.
 *
 * <p>유형 ③(status 없음)이라 조회에 status 조건이 없습니다.
 * 필터 조건도 없어서 {@code JpaRepository} 기본 메서드 {@code findAll(pageable)}·{@code findById} 만으로 충분합니다.
 *
 * <p>property_id 는 조회 조건에 넣지 않습니다(정책 32행).
 */
public interface PropertyImageRepository extends JpaRepository<PropertyImage, Long> {
}
