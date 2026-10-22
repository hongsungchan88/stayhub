package com.stayhub.content.property.repository;

import com.stayhub.content.property.entity.Property;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 숙소 DB 접근.
 *
 * <p>유형 ②(업무 상태 있음)라 조회에 status 조건을 붙이지 않습니다.
 * 필터 조건도 없어서 {@code JpaRepository} 기본 메서드 {@code findAll(pageable)}·{@code findById} 만으로 충분합니다.
 * 정렬은 서비스가 {@code Pageable} 에 담아 넘깁니다.
 */
public interface PropertyRepository extends JpaRepository<Property, Long> {
}
