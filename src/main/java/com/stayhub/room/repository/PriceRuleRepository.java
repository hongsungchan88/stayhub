package com.stayhub.room.repository;

import com.stayhub.room.entity.PriceRule;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 동적 가격 규칙 DB 접근. [v2 이관 · 스키마만 유지]
 *
 * <p>1차는 테이블만 만들어 둡니다. 서비스·API 는 v2 에서 이 리포지토리를 써서 만듭니다.
 */
public interface PriceRuleRepository extends JpaRepository<PriceRule, Long> {
}
