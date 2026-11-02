package com.stayhub.reservation.repository;

import com.stayhub.reservation.entity.Reservation;
import com.stayhub.reservation.entity.ReservationStatus;
import java.time.LocalDate;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 예약 DB 접근.
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>저장·ID 조회는 {@code JpaRepository} 기본 메서드를 씁니다(유형 ②).
 * 직접 선언한 것은 날짜 겹침 확인 하나뿐입니다. 목록·통합 검색(RSV-006)은 담당자(B)가 추가합니다.
 */
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

	/**
	 * 같은 객실에 날짜가 겹치는 예약이 있는지. {@code excluded} 상태(취소·노쇼)는 빼고 봅니다.
	 *
	 * <p>겹침 조건: 기존 체크인 &lt; 새 체크아웃 AND 기존 체크아웃 &gt; 새 체크인.
	 * 등호가 없어서 "앞 예약의 체크아웃 날 = 새 예약의 체크인 날" 은 겹치지 않는 것으로 봅니다.
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
			@Param("excluded") Collection<ReservationStatus> excluded);
}
