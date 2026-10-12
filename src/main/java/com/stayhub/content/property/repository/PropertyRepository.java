package com.stayhub.content.property.repository;

import com.stayhub.content.property.entity.Property;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 숙소 DB 접근.
 *
 * <p>유형 ②(업무 상태 있음)라서 조회에 {@code status} 조건을 붙이지 않습니다.
 * 필터 조건도 없으므로 {@code JpaRepository} 기본 메서드 {@code findAll(pageable)}·{@code findById} 로 충분해
 * 여기에 메서드를 따로 선언하지 않습니다.
 * 정렬은 여기서 정하지 않고 서비스가 {@code Pageable} 에 담아 넘깁니다.
 */
public interface PropertyRepository extends JpaRepository<Property, Long> {
}
