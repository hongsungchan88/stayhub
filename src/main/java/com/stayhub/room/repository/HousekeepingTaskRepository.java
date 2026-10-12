package com.stayhub.room.repository;

import com.stayhub.room.entity.HousekeepingTask;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 청소/점검 작업 스케줄 DB 접근.
 *
 * <p>유형 ②(업무 상태 있음) 테이블이고 목록 필터가 없어서, {@code JpaRepository} 기본 메서드
 * {@code findAll(pageable)}·{@code findById}·{@code save} 로 충분합니다. 따로 선언한 메서드가 없습니다.
 */
public interface HousekeepingTaskRepository extends JpaRepository<HousekeepingTask, Long> {
}
