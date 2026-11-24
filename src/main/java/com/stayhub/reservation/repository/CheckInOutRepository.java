package com.stayhub.reservation.repository;

import com.stayhub.reservation.entity.CheckInOut;
import com.stayhub.reservation.entity.Reservation;
import com.stayhub.reservation.entity.ReservationStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CheckInOutRepository extends JpaRepository<CheckInOut, Long> {
    Optional<CheckInOut> findByReservationId(Long reservationId);

    @Query("""
        select r from Reservation r
        where r.status = :inHouse or
             (r.status = :confirmed and r.checkInDate <= :today and r.checkOutDate > :today)
        order by r.checkInDate, r.id
        """)
    Page<Reservation> findDeskRows(@Param("inHouse") ReservationStatus inHouse,
                                   @Param("confirmed") ReservationStatus confirmed, @Param("today") LocalDate today,
                                   Pageable pageable);

    // 미정산 잔액이 아니라 정산된 추가 요금 총액을 입퇴실 기록에 보존한다.
    @Query("select sum(c.amount) from AdditionalCharge c where c.reservationId = :id and c.isSettled = true")
    BigDecimal sumSettledFees(@Param("id") Long id);
}