package com.stayhub.reservation.service;

import com.stayhub.common.BusinessException;
import com.stayhub.common.PageResponse;
import com.stayhub.payment.service.AdditionalChargeService;
import com.stayhub.reservation.entity.*;
import com.stayhub.reservation.repository.CheckInOutRepository;
import com.stayhub.room.dto.RoomStatusChangeRequest;
import com.stayhub.room.entity.RoomStatus;
import com.stayhub.room.service.HousekeepingTaskService;
import com.stayhub.room.service.RoomService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CheckInOutService {
    private final CheckInOutRepository stays;
    private final ReservationService reservations;
    private final RoomService rooms;
    private final AdditionalChargeService charges;
    private final HousekeepingTaskService cleaning;
    private final EntityManager em;

    public record DeskRow(Long id, String bookingCode, String guestName, String roomNumber,
                          LocalDate arrival, LocalDate departure, boolean inHouse,
                          boolean vacant, boolean unpaid) {}

    public PageResponse<DeskRow> list(int page) {
        if (page < 0) throw new BusinessException("페이지 번호가 올바르지 않습니다.");
        return PageResponse.from(stays.findDeskRows(ReservationStatus.CHECKED_IN,
                ReservationStatus.CONFIRMED, LocalDate.now(),
                PageRequest.of(page, PageResponse.DEFAULT_PAGE_SIZE)).map(r -> {
            var room = rooms.getDetail(r.getRoomId());
            boolean inHouse = r.getStatus() == ReservationStatus.CHECKED_IN;
            return new DeskRow(r.getId(), r.getReservationCode(), r.getGuestName(),
                    room.roomNumber(), r.getCheckInDate(), r.getCheckOutDate(), inHouse,
                    room.status() == RoomStatus.VACANT,
                    inHouse && charges.hasUnsettledCharges(r.getId()));
        }));
    }

    private Reservation lockReservation(Long id) {
        var r = em.find(Reservation.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (r == null) throw new BusinessException("예약을 찾을 수 없습니다.");
        return r;
    }

    @Transactional
    public void checkIn(Long id, String key, Long staffId) {
        var r = lockReservation(id);
        if (r.getStatus() != ReservationStatus.CONFIRMED)
            throw new BusinessException("예약 확정 상태에서만 체크인할 수 있습니다.");
        var now = LocalDateTime.now();
        var today = now.toLocalDate();
        if (today.isBefore(r.getCheckInDate()) || !today.isBefore(r.getCheckOutDate()))
            throw new BusinessException("예약된 숙박 기간 안에서만 체크인할 수 있습니다.");
        if (key == null || key.isBlank() || key.length() > 50)
            throw new BusinessException("키 번호를 1~50자로 입력해 주세요.");
        if (rooms.getDetail(r.getRoomId()).status() != RoomStatus.VACANT)
            throw new BusinessException("빈 객실만 체크인할 수 있습니다.");
        reservations.changeStatus(id, ReservationStatus.CHECKED_IN);
        rooms.changeStatus(r.getRoomId(), new RoomStatusChangeRequest(
                RoomStatus.VACANT, RoomStatus.OCCUPIED, "체크인"), staffId);
        stays.save(CheckInOut.start(id, key.strip(), now));
    }

    @Transactional
    public void checkOut(Long id, Long staffId) {
        var r = lockReservation(id);
        if (r.getStatus() != ReservationStatus.CHECKED_IN)
            throw new BusinessException("투숙 중인 예약만 체크아웃할 수 있습니다.");
        var stay = stays.findByReservationId(id)
                .orElseThrow(() -> new BusinessException("입실 기록이 없습니다."));
        charges.checkAllSettled(id);
        var total = stays.sumSettledFees(id);
        var now = LocalDateTime.now();
        reservations.changeStatus(id, ReservationStatus.CHECKED_OUT);
        stay.finish(total == null ? BigDecimal.ZERO : total, now);
        cleaning.createCheckoutCleaning(r.getRoomId(), now, staffId);
    }
}