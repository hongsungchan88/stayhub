package com.stayhub.statistics.repository;

import com.stayhub.channel.entity.Channel;
import com.stayhub.payment.entity.Payment;
import com.stayhub.reservation.entity.Reservation;
import com.stayhub.reservation.entity.ReservationStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * 통계 계산용 조회 (STA-001~004). <b>읽기 전용</b>입니다.
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>통계는 따로 저장하지 않고 조회할 때마다 계산합니다(총괄 결정 — 숙소가 5~30실이라 매번 계산해도 빠름).
 * 그래서 다른 도메인의 rooms·reservations·payments·channels 를 JPQL 로 읽기만 합니다.
 * statistics 폴더 밖의 파일은 건드리지 않습니다(statistics/package-info).
 *
 * <p><b>JpaRepository 가 아니라 {@link Repository} 를 상속하는 이유</b>
 * <br>JpaRepository 를 상속하면 save·delete 가 생겨서, 이 리포지토리로 예약을 저장할 수 있게 됩니다.
 * 예약은 겹침 검사·동시 예약 차단 때문에 반드시 ReservationService.create 로만 만들어야 하므로,
 * 저장 메서드가 없는 {@link Repository} 로 두고 아래 조회만 선언합니다.
 */
public interface StatisticsRepository extends Repository<Reservation, Long> {

	/** 판매 가능 객실 수 = rooms 전체 개수 (총괄 결정 9). */
	@Query("select count(r) from Room r")
	long countRooms();

	/**
	 * 조회 기간(from~to, 둘 다 포함)에 하룻밤이라도 투숙하는 예약.
	 * 투숙 = check_in ≤ 그날 < check_out 이라서, 겹치려면 check_in ≤ to 이고 check_out > from 입니다.
	 *
	 * @param statuses 판매로 셀 상태 (CONFIRMED·CHECKED_IN·CHECKED_OUT, 총괄 결정 8)
	 */
	@Query("select r from Reservation r"
			+ " where r.status in :statuses and r.checkInDate <= :to and r.checkOutDate > :from")
	List<Reservation> findStays(
			@Param("from") LocalDate from,
			@Param("to") LocalDate to,
			@Param("statuses") Collection<ReservationStatus> statuses);

	/** 예약을 만든 시각이 [start, end) 인 예약 중 excluded 상태가 아닌 것 (총괄 결정 10 — CANCELLED 만 제외). */
	@Query("select r from Reservation r"
			+ " where r.createdAt >= :start and r.createdAt < :end and r.status <> :excluded")
	List<Reservation> findCreatedBetween(
			@Param("start") LocalDateTime start,
			@Param("end") LocalDateTime end,
			@Param("excluded") ReservationStatus excluded);

	/** 결제 시각이 [start, end) 인 결제 전부. 상태별 매출 계산은 서비스가 합니다(총괄 결정 3). */
	@Query("select p from Payment p where p.createdAt >= :start and p.createdAt < :end")
	List<Payment> findPaymentsBetween(
			@Param("start") LocalDateTime start,
			@Param("end") LocalDateTime end);

	/** 채널 이름표. 1번 = 직접 예약, 2번 = 기타 채널 (data.sql). */
	@Query("select c from Channel c order by c.id")
	List<Channel> findAllChannels();
}
