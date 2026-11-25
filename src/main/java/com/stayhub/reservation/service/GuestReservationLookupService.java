package com.stayhub.reservation.service;

import com.stayhub.common.BusinessException;
import com.stayhub.reservation.dto.GuestReservationLookupRequest;
import com.stayhub.reservation.dto.GuestReservationLookupResponse;
import com.stayhub.reservation.entity.Reservation;
import com.stayhub.reservation.repository.ReservationRepository;
import com.stayhub.room.entity.Room;
import com.stayhub.room.repository.RoomRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 예약번호와 연락처가 일치하는 예약을 확인하고 게스트용 상세를 조회합니다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuestReservationLookupService {

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    /** 일치하면 서버 세션에 보관할 내부 예약 ID를 반환합니다. */
    public Long verify(GuestReservationLookupRequest request) {
        return reservationRepository.findByReservationCodeIgnoreCaseAndGuestPhone(
                        request.reservationCode().strip(), request.guestPhone().strip())
                .map(Reservation::getId)
                .orElseThrow(() -> new BusinessException("예약번호와 연락처를 다시 확인해 주세요."));
    }

    /** 컨트롤러가 확인한 세션의 예약만 조회합니다. 연락처와 내부 ID는 응답에서 제외합니다. */
    public Optional<GuestReservationLookupResponse> findDetail(Long verifiedReservationId) {
        return reservationRepository.findById(verifiedReservationId)
                .map(reservation -> new GuestReservationLookupResponse(
                        reservation.getReservationCode(),
                        roomRepository.findById(reservation.getRoomId())
                                .map(Room::getRoomNumber).orElse("-"),
                        reservation.getCheckInDate(), reservation.getCheckOutDate(),
                        reservation.getGuestCount(), reservation.getStatus()));
    }
}
