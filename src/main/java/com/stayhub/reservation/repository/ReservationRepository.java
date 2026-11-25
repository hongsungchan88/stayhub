package com.stayhub.reservation.repository;

import com.stayhub.reservation.entity.Reservation;
import com.stayhub.reservation.entity.ReservationStatus;
import java.time.LocalDate;
import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 예약 DB 접근.
 *
 * <p>기존 날짜 겹침 검사와 목록·통합 검색에 필요한 메서드입니다.
 * 조회할 때 취소·노쇼 예약도 목록에 포함합니다.
 */
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

	/**
	 * 같은 객실에 날짜가 겹치는 예약이 있는지 확인합니다.
	 * 취소·노쇼 등 excluded에 전달한 상태는 제외합니다.
	 *
	 * <p>기존 체크아웃 날짜와 새 체크인 날짜가 같으면 겹치지 않습니다.
	 */
	@Query("""
            select count(r) > 0 from Reservation r
            where r.roomId = :roomId
              and r.status not in :excluded
              and r.checkInDate < :checkOutDate
              and r.checkOutDate > :checkInDate
            """)
	boolean existsOverlap(
			@Param("roomId") Long roomId,
			@Param("checkInDate") LocalDate checkInDate,
			@Param("checkOutDate") LocalDate checkOutDate,
			@Param("excluded") Collection<ReservationStatus> excluded
	);

	/** 예약자 이름으로 부분 검색합니다. */
	Page<Reservation> findByGuestNameContainingIgnoreCase(
			String keyword, Pageable pageable
	);

	/** 연락처로 부분 검색합니다. */
	Page<Reservation> findByGuestPhoneContainingIgnoreCase(
			String keyword, Pageable pageable
	);

	/** 예약번호로 부분 검색합니다. */
	Page<Reservation> findByReservationCodeContainingIgnoreCase(
			String keyword, Pageable pageable
	);

	/** 전달받은 채널 ID 중 하나에 해당하는 예약을 검색합니다. */
	Page<Reservation> findByChannelIdIn(
			Collection<Long> channelIds, Pageable pageable
	);

	/** 채널 ID가 없는 직접 예약을 검색합니다. */
	Page<Reservation> findByChannelIdIsNull(Pageable pageable);

	/** 해당 채널의 예약과 직접 예약을 함께 검색합니다. */
	Page<Reservation> findByChannelIdInOrChannelIdIsNull(
			Collection<Long> channelIds, Pageable pageable
	);

	/**
	 * 예약 변경 시 자기 자신을 제외하고 날짜 겹침을 검사합니다.
	 * 기존 예약을 제외하지 않으면 자기 예약과 겹친다고 판단하게 됩니다.
	 */
	@Query("""
            select count(r) > 0 from Reservation r
            where r.roomId = :roomId
              and r.id <> :reservationId
              and r.status not in :excluded
              and r.checkInDate < :checkOutDate
              and r.checkOutDate > :checkInDate
            """)
	boolean existsOverlapExcludingReservation(
			@Param("roomId") Long roomId,
			@Param("reservationId") Long reservationId,
			@Param("checkInDate") LocalDate checkInDate,
			@Param("checkOutDate") LocalDate checkOutDate,
			@Param("excluded") Collection<ReservationStatus> excluded
	);

	/** 오늘 또는 과거에 체크인 예정이었던 확정 예약을 조회합니다. */
	Page<Reservation> findByStatusAndCheckInDateLessThanEqual(
			ReservationStatus status,
			LocalDate checkInDate,
			Pageable pageable
	);
}