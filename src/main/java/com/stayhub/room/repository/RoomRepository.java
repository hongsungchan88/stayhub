package com.stayhub.room.repository;

import com.stayhub.room.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 객실 DB 접근.
 *
 * <p>유형 ②(업무 상태 있음) 테이블이고 목록 필터가 없어서, {@code JpaRepository} 기본 메서드
 * {@code findAll(pageable)}·{@code findById}·{@code save} 로 충분합니다. 따로 선언한 메서드가 없습니다.
 *
 * <p><b>property_id 는 조회 조건에 넣지 않습니다.</b> (정책 32행)
 */
public interface RoomRepository extends JpaRepository<Room, Long> {
}
