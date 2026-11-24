package com.stayhub.reservation;

import com.stayhub.common.BusinessException;
import com.stayhub.payment.entity.*;
import com.stayhub.payment.repository.AdditionalChargeRepository;
import com.stayhub.reservation.entity.*;
import com.stayhub.reservation.repository.*;
import com.stayhub.reservation.service.CheckInOutService;
import com.stayhub.room.entity.Room;
import com.stayhub.room.entity.RoomStatus;
import com.stayhub.room.repository.RoomRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class CheckInOutServiceTest {
    @Autowired CheckInOutService desk;
    @Autowired RoomRepository rooms;
    @Autowired ReservationRepository reservations;
    @Autowired CheckInOutRepository stays;
    @Autowired AdditionalChargeRepository charges;
    @Autowired EntityManager em;
    Long roomId;
    LocalDate today = LocalDate.now();

    @BeforeEach void room() {
        roomId = rooms.saveAndFlush(Room.create(1L, "CI" + token(), "STANDARD",
                new BigDecimal("100000"), 2)).getId();
    }
    String token() { return UUID.randomUUID().toString().substring(0, 8); }
    Long booking(LocalDate start, LocalDate end) {
        // 과거에 생성돼 있던 예약도 재현하기 위해 테스트 픽스처는 직접 저장합니다.
        return reservations.saveAndFlush(Reservation.create("T" + token(), null,
                "입실테스트", "010-0000-0000", roomId, null, start, end, 1)).getId();
    }
    long cleaningCount() {
        return em.createQuery("select count(h) from HousekeepingTask h where h.roomId = :id",
                Long.class).setParameter("id", roomId).getSingleResult();
    }
    @Test void lateArrivalAndSettledCheckout() {
        Long id = booking(today.minusDays(1), today.plusDays(1));
        desk.checkIn(id, " K-101 ", 1L);
        assertThat(stays.findByReservationId(id).orElseThrow().getKeyCode()).isEqualTo("K-101");
        assertThat(rooms.findById(roomId).orElseThrow().getStatus()).isEqualTo(RoomStatus.OCCUPIED);
        charges.saveAndFlush(AdditionalCharge.create(id, AdditionalChargeType.MINIBAR,
                "물", new BigDecimal("5000"), false, null, true));
        desk.checkOut(id, 1L);
        em.flush(); em.clear();
        var stay = stays.findByReservationId(id).orElseThrow();
        assertThat(stay.getActualCheckIn()).isNotNull();
        assertThat(stay.getActualCheckOut()).isNotNull();
        assertThat(stay.getExtraFee()).isEqualByComparingTo("5000");
        assertThat(stay.isSettled()).isTrue();
        assertThat(reservations.findById(id).orElseThrow().getStatus()).isEqualTo(ReservationStatus.CHECKED_OUT);
        assertThat(rooms.findById(roomId).orElseThrow().getStatus()).isEqualTo(RoomStatus.CLEANING_NEEDED);
        assertThat(cleaningCount()).isOne();
        assertThatThrownBy(() -> desk.checkOut(id, 1L)).isInstanceOf(BusinessException.class);
        assertThat(cleaningCount()).isOne();
    }
    @Test void unpaidBlocksCheckout() {
        Long id = booking(today, today.plusDays(1));
        desk.checkIn(id, "A", 1L);
        charges.saveAndFlush(AdditionalCharge.create(id, AdditionalChargeType.MINIBAR,
                "물", BigDecimal.TEN, false, null, false));
        assertThatThrownBy(() -> desk.checkOut(id, 1L)).isInstanceOf(BusinessException.class);
        assertThat(reservations.findById(id).orElseThrow().getStatus()).isEqualTo(ReservationStatus.CHECKED_IN);
        assertThat(stays.findByReservationId(id).orElseThrow().getActualCheckOut()).isNull();
        assertThat(rooms.findById(roomId).orElseThrow().getStatus()).isEqualTo(RoomStatus.OCCUPIED);
        assertThat(cleaningCount()).isZero();
    }
    @Test void noEarlyArrival() {
        Long id = booking(today.plusDays(1), today.plusDays(2));
        assertThatThrownBy(() -> desk.checkIn(id, "A", 1L)).isInstanceOf(BusinessException.class);
        assertThat(stays.findByReservationId(id)).isEmpty();
    }
    @Test void noArrivalOnDepartureDay() {
        Long id = booking(today.minusDays(1), today);
        assertThatThrownBy(() -> desk.checkIn(id, "A", 1L)).isInstanceOf(BusinessException.class);
    }
    @Test void doubleCheckInIsRejected() {
        Long id = booking(today, today.plusDays(1));
        desk.checkIn(id, "A", 1L);
        assertThatThrownBy(() -> desk.checkIn(id, "B", 1L)).isInstanceOf(BusinessException.class);
        assertThat(stays.findByReservationId(id).orElseThrow().getKeyCode()).isEqualTo("A");
    }
}