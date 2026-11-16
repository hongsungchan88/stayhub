package com.stayhub.reservation.service;

import com.stayhub.common.BusinessException;
import com.stayhub.reservation.dto.PublicAvailabilityResponse;
import com.stayhub.reservation.dto.PublicReservationRequest;
import com.stayhub.reservation.dto.PublicReservationResponse;
import com.stayhub.reservation.entity.Reservation;
import com.stayhub.reservation.entity.ReservationStatus;
import com.stayhub.reservation.repository.ReservationRepository;
import com.stayhub.room.entity.Room;
import com.stayhub.room.repository.RoomRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게스트(비로그인) 예약. CMS-008 빈 객실 조회 + SCR-GST-03 예약하기.
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>빈 객실 조회는 기존 {@link ReservationRepository#existsOverlap}(취소·노쇼 제외)과 객실 최대 인원으로 거릅니다.
 * 예약 생성은 {@link ReservationService#create} 를 그대로 부릅니다(객실 락 → 겹침 검사 → 인원 검사가 거기 있음).
 * 겹침·인원 규칙을 여기 새로 만들지 마세요.
 * 새 규칙은 게스트 입력 검증 하나뿐입니다: 체크인 ≥ 오늘, 체크아웃 > 체크인, 최대 30박, 인원 ≥ 1.
 *
 * <p>가격 = rooms.base_price × 박수 (동적 요금 ROM-002 는 v2). 결제는 1차에 없습니다(체크인 때 현장 결제).
 * 게스트 예약은 user_id·channel_id 모두 NULL — 채널 NULL 은 목록·통계에서 "직접 예약"으로 묶입니다.
 */
@Service
@Transactional(readOnly = true)
public class PublicReservationService {

	static final int MAX_NIGHTS = 30;
	static final int ALTERNATIVE_SEARCH_DAYS = 14;
	static final int ALTERNATIVE_LIMIT = 3;

	private final ReservationService reservationService;
	private final ReservationRepository reservationRepository;
	private final RoomRepository roomRepository;

	public PublicReservationService(
			ReservationService reservationService,
			ReservationRepository reservationRepository,
			RoomRepository roomRepository
	) {
		this.reservationService = reservationService;
		this.reservationRepository = reservationRepository;
		this.roomRepository = roomRepository;
	}

	/**
	 * 빈 객실 목록. 없으면 같은 박수로 체크인을 하루씩 늦춰 14일 안에서 빈 객실이 있는 첫 날짜 3개를 함께 돌려줍니다.
	 */
	public PublicAvailabilityResponse getAvailability(LocalDate checkIn, LocalDate checkOut, Integer guests) {
		validate(checkIn, checkOut, guests);
		long nights = ChronoUnit.DAYS.between(checkIn, checkOut);

		// shortcut: 객실 전체를 읽고 객실마다 겹침 쿼리를 한 번씩 부릅니다(대체 날짜까지 최대 15회 × 객실 수). 객실이 수십 개를 넘으면 한 번에 모아 세는 쿼리로 바꾸기
		List<Room> rooms = roomRepository.findAll().stream()
				.filter(room -> room.getMaxOccupancy() >= guests)
				.toList();

		List<PublicAvailabilityResponse.AvailableRoom> available = freeRooms(rooms, checkIn, checkOut).stream()
				.map(room -> PublicAvailabilityResponse.AvailableRoom.of(room, nights))
				.toList();

		List<PublicAvailabilityResponse.AlternativeDate> alternatives = new ArrayList<>();
		for (int day = 1; available.isEmpty() && day <= ALTERNATIVE_SEARCH_DAYS
				&& alternatives.size() < ALTERNATIVE_LIMIT; day++) {
			LocalDate in = checkIn.plusDays(day);
			LocalDate out = in.plusDays(nights);
			if (!freeRooms(rooms, in, out).isEmpty()) {
				alternatives.add(new PublicAvailabilityResponse.AlternativeDate(in, out));
			}
		}
		return new PublicAvailabilityResponse(available, alternatives);
	}

	/**
	 * 게스트 예약 생성. 기존 {@link ReservationService#create} 를 userId·channelId 모두 null 로 부릅니다.
	 *
	 * <p>shortcut: 같은 사람(연락처)의 반복 예약 횟수 제한이 없습니다. 2차에서 연락처·IP 기준 제한을 넣기
	 */
	@Transactional
	public PublicReservationResponse create(PublicReservationRequest request) {
		validate(request.checkIn(), request.checkOut(), request.guestCount());

		Reservation reservation = reservationService.create(
				null,
				request.guestName(),
				request.guestPhone(),
				request.roomId(),
				null,
				request.checkIn(),
				request.checkOut(),
				request.guestCount()
		);
		Room room = roomRepository.findById(reservation.getRoomId()).orElseThrow();
		return PublicReservationResponse.of(reservation, room);
	}

	private List<Room> freeRooms(List<Room> rooms, LocalDate checkIn, LocalDate checkOut) {
		return rooms.stream()
				.filter(room -> !reservationRepository.existsOverlap(
						room.getId(), checkIn, checkOut, ReservationStatus.RELEASED))
				.toList();
	}

	private void validate(LocalDate checkIn, LocalDate checkOut, Integer guests) {
		if (checkIn == null || checkIn.isBefore(LocalDate.now())) {
			throw new BusinessException("checkIn: 체크인 날짜는 오늘 이후로 선택해 주세요.");
		}
		if (checkOut == null || !checkOut.isAfter(checkIn)) {
			throw new BusinessException("checkOut: 체크아웃 날짜는 체크인 날짜보다 뒤여야 합니다.");
		}
		if (ChronoUnit.DAYS.between(checkIn, checkOut) > MAX_NIGHTS) {
			throw new BusinessException("checkOut: 최대 " + MAX_NIGHTS + "박까지 예약할 수 있습니다.");
		}
		if (guests == null || guests < 1) {
			throw new BusinessException("guestCount: 투숙 인원은 1명 이상이어야 합니다.");
		}
	}
}
