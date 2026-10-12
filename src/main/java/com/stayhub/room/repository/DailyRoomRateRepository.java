package com.stayhub.room.repository;

import com.stayhub.room.entity.DailyRoomRate;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 일별 객실 요금표 DB 접근. [v2 이관 · 스키마만 유지]
 *
 * <p>1차는 테이블만 만들어 둡니다. 서비스·API 는 v2 에서 이 리포지토리를 써서 만듭니다.
 */
public interface DailyRoomRateRepository extends JpaRepository<DailyRoomRate, Long> {
}
