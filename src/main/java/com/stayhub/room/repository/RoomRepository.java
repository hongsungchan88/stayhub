package com.stayhub.room.repository;

import com.stayhub.room.entity.Room;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

/**
 * 객실 DB 접근.
 *
 * <p>유형 ②(업무 상태 있음) 테이블이고 목록 필터가 없어서, 목록·상세는 {@code JpaRepository} 기본 메서드
 * {@code findAll(pageable)}·{@code findById} 를 씁니다.
 * 따로 선언한 메서드는 room_number UNIQUE 를 미리 확인하는 용도와, 예약 생성 때 쓰는 잠금 조회입니다.
 *
 * <p><b>property_id 는 조회 조건에 넣지 않습니다.</b> (정책 32행)
 */
public interface RoomRepository extends JpaRepository<Room, Long> {

	/** 같은 객실 번호가 이미 있는지. (등록할 때) */
	boolean existsByRoomNumber(String roomNumber);

	/** 나 말고 같은 객실 번호를 쓰는 객실이 있는지. (수정할 때 — 자기 번호 그대로 두는 건 허용) */
	boolean existsByRoomNumberAndIdNot(String roomNumber, Long id);

	/**
	 * 예약 생성 전용. 이 트랜잭션이 끝날 때 객실 version 을 하나 올립니다({@code OPTIMISTIC_FORCE_INCREMENT}).
	 * 같은 객실을 동시에 예약하면 나중 쪽이 version 충돌(409)로 실패합니다. 사용처: {@code ReservationService.create}.
	 * 객실 상태 변경 같은 다른 곳에서는 쓰지 마세요. 필요 없이 version 이 올라 다른 요청이 409 를 받습니다.
	 */
	@Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
	Optional<Room> findForReservationById(Long id);
}
