package com.stayhub.room.repository;

import com.stayhub.room.entity.RoomStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 객실 상태 변경 이력 DB 접근.
 *
 * <p>소프트 삭제용 status 가 없고 목록 필터도 없어서, {@code JpaRepository} 기본 메서드
 * {@code findAll(pageable)}·{@code findById}·{@code save} 로 충분합니다. 따로 선언한 메서드가 없습니다.
 */
public interface RoomStatusLogRepository extends JpaRepository<RoomStatusLog, Long> {
}
