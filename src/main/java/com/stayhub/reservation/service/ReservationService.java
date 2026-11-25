package com.stayhub.reservation.service;

import com.stayhub.channel.entity.Channel;
import com.stayhub.channel.repository.ChannelRepository;
import com.stayhub.common.BusinessException;
import com.stayhub.common.PageResponse;
import com.stayhub.reservation.dto.ReservationCreateRequest;
import com.stayhub.reservation.dto.ReservationResponse;
import com.stayhub.reservation.entity.Reservation;
import com.stayhub.reservation.entity.ReservationStatus;
import com.stayhub.reservation.repository.ReservationRepository;
import com.stayhub.room.entity.Room;
import com.stayhub.room.repository.RoomRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.stayhub.reservation.dto.ReservationUpdateRequest;
import com.stayhub.payment.service.PaymentService;

/**
 * 예약 생성·상태 변경·목록 조회·통합 검색.
 *
 * <p>총괄의 중복 예약 방지 틀을 사용합니다.
 * 예약 생성은 반드시 이 서비스의 create를 거칩니다.
 * 객실의 낙관적 락과 날짜 겹침 검사를 건너뛰어 직접 저장하지 않습니다.
 */
@Service
@Transactional(readOnly = true)
public class ReservationService {

	private static final DateTimeFormatter CODE_DATE =
			DateTimeFormatter.ofPattern("yyyyMMdd");

	private final ReservationRepository reservationRepository;
	private final RoomRepository roomRepository;
	private final ChannelRepository channelRepository;
	private final PaymentService paymentService;

	public ReservationService(
			ReservationRepository reservationRepository,
			RoomRepository roomRepository,
			ChannelRepository channelRepository,
			PaymentService paymentService
	) {
		this.reservationRepository = reservationRepository;
		this.roomRepository = roomRepository;
		this.channelRepository = channelRepository;
		this.paymentService = paymentService;
	}

	/**
	 * 예약 생성의 공통 진입점.
	 * 회원 예약이면 userId를 전달하고, 비회원이면 null을 전달합니다.
	 */
	@Transactional
	public Reservation create(
			Long userId,
			String guestName,
			String guestPhone,
			Long roomId,
			Long channelId,
			LocalDate checkInDate,
			LocalDate checkOutDate,
			Integer guestCount
	) {
		if (checkInDate == null || checkOutDate == null
				|| !checkOutDate.isAfter(checkInDate)) {
			throw new BusinessException(
					"체크아웃 날짜는 체크인 날짜보다 뒤여야 합니다.");
		}

		if (checkInDate.isBefore(LocalDate.now())) {
			throw new BusinessException(
					"checkInDate: 체크인 날짜는 오늘 또는 이후여야 합니다.");
		}

		// 총괄의 동시 예약 차단: 겹침 검사 전에 객실의 낙관적 락을 잡습니다.
		Room room = roomRepository.findForReservationById(roomId)
				.orElseThrow(() -> new BusinessException("객실을 찾을 수 없습니다."));

		if (reservationRepository.existsOverlap(
				roomId, checkInDate, checkOutDate, ReservationStatus.RELEASED)) {
			throw new BusinessException("선택한 날짜에 이미 예약된 객실입니다.");
		}

		int count = guestCount == null ? 1 : guestCount;
		if (count < 1 || count > room.getMaxOccupancy()) {
			throw new BusinessException(
					"guestCount: 투숙 인원은 1명 이상, 객실 최대 인원 "
							+ room.getMaxOccupancy() + "명 이하여야 합니다.");
		}

		if (channelId != null && !channelRepository.existsById(channelId)) {
			throw new BusinessException("channelId: 예약 채널을 찾을 수 없습니다.");
		}

		Reservation reservation = Reservation.create(
				newReservationCode(checkInDate),
				userId, guestName, guestPhone, roomId, channelId,
				checkInDate, checkOutDate, guestCount
		);
		return reservationRepository.save(reservation);
	}

	/**
	 * 화면에서 받은 등록 요청을 처리합니다.
	 * 이번 관리자 등록은 회원 연결 없이 비회원 예약으로 생성합니다.
	 * 위의 공통 create를 호출하므로 중복 예약 검사를 그대로 거칩니다.
	 */
	@Transactional
	public ReservationResponse create(ReservationCreateRequest request) {
		Reservation reservation = create(
				null,
				request.guestName(),
				request.guestPhone(),
				request.roomId(),
				request.channelId(),
				request.checkInDate(),
				request.checkOutDate(),
				request.guestCount()
		);

		String roomNumber = roomRepository.findById(reservation.getRoomId())
				.orElseThrow().getRoomNumber();

		String channelName = reservation.getChannelId() == null
				? "직접 예약"
				: channelRepository.findById(reservation.getChannelId())
				.orElseThrow().getChannelName();

		return ReservationResponse.from(reservation, roomNumber, channelName);
	}

	/** 예약 상세 조회. 취소·노쇼 예약도 조회할 수 있습니다. */
	public ReservationResponse getDetail(Long id) {
		Reservation reservation = reservationRepository.findById(id)
				.orElseThrow(() ->
						new BusinessException("예약을 찾을 수 없습니다."));

		String roomNumber = roomRepository.findById(reservation.getRoomId())
				.map(Room::getRoomNumber)
				.orElse("객실 #" + reservation.getRoomId());

		String channelName = reservation.getChannelId() == null
				? "직접 예약"
				: channelRepository.findById(reservation.getChannelId())
				.map(Channel::getChannelName)
				.orElse("채널 #" + reservation.getChannelId());

		return ReservationResponse.from(
				reservation, roomNumber, channelName);
	}

	/**
	 * 예약 날짜·객실 변경.
	 * 기존 예약을 제외한 중복 검사와 객실의 낙관적 락을 사용합니다.
	 */
	@Transactional
	public ReservationResponse update(
			Long id, ReservationUpdateRequest request
	) {
		Reservation reservation = reservationRepository.findById(id)
				.orElseThrow(() ->
						new BusinessException("예약을 찾을 수 없습니다."));

		if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
			throw new BusinessException(
					"예약 확정 상태에서만 날짜와 객실을 변경할 수 있습니다.");
		}

		LocalDate today = LocalDate.now();

		if (!reservation.getCheckInDate().isAfter(today)) {
			throw new BusinessException(
					"체크인 당일 또는 지난 예약은 변경할 수 없습니다.");
		}

		if (request.checkInDate() == null
				|| request.checkOutDate() == null
				|| !request.checkOutDate().isAfter(request.checkInDate())) {
			throw new BusinessException(
					"checkOutDate: 체크아웃 날짜는 체크인 날짜보다 뒤여야 합니다.");
		}

		if (request.checkInDate().isBefore(today)) {
			throw new BusinessException(
					"checkInDate: 변경할 체크인 날짜는 오늘 또는 이후여야 합니다.");
		}

		if (request.roomId() == null) {
			throw new BusinessException("roomId: 객실을 선택해 주세요.");
		}

		// 새로 배정할 객실에 대한 동시 예약·변경을 차단합니다.
		Room room = roomRepository.findForReservationById(request.roomId())
				.orElseThrow(() ->
						new BusinessException("roomId: 객실을 찾을 수 없습니다."));

		// 인원은 그대로 유지하므로 변경할 객실에도 수용 가능한지 확인합니다.
		if (reservation.getGuestCount() > room.getMaxOccupancy()) {
			throw new BusinessException(
					"roomId: 예약 인원이 선택한 객실의 최대 인원 "
							+ room.getMaxOccupancy() + "명을 초과합니다.");
		}

		if (reservationRepository.existsOverlapExcludingReservation(
				request.roomId(),
				reservation.getId(),
				request.checkInDate(),
				request.checkOutDate(),
				ReservationStatus.RELEASED
		)) {
			throw new BusinessException(
					"선택한 날짜에 이미 예약된 객실입니다.");
		}

		reservation.changeSchedule(
				request.roomId(),
				request.checkInDate(),
				request.checkOutDate()
		);

		// 수정일시를 반영한 뒤 응답 DTO를 만듭니다.
		reservationRepository.flush();
		return getDetail(reservation.getId());
	}

	/** 목록·통합 검색. 최신 예약부터 한 페이지에 20건씩 조회합니다. */
	public PageResponse<ReservationResponse> getList(
			String searchType, String keyword, int page
	) {
		if (page < 0) {
			throw new BusinessException("페이지 번호는 0 이상이어야 합니다.");
		}

		if (searchType == null || !Set.of(
				"guestName", "guestPhone", "reservationCode", "channelName"
		).contains(searchType)) {
			throw new BusinessException("검색 항목이 올바르지 않습니다.");
		}

		String term = keyword == null ? "" : keyword.trim();
		if (term.length() > 100) {
			throw new BusinessException("검색어는 100자 이내로 입력해 주세요.");
		}

		Pageable pageable = PageRequest.of(
				page,
				PageResponse.DEFAULT_PAGE_SIZE,
				Sort.by(Sort.Direction.DESC, "id")
		);

		Page<Reservation> reservations;
		if (term.isEmpty()) {
			reservations = reservationRepository.findAll(pageable);
		} else {
			reservations = switch (searchType) {
				case "guestName" ->
						reservationRepository.findByGuestNameContainingIgnoreCase(
								term, pageable);
				case "guestPhone" ->
						reservationRepository.findByGuestPhoneContainingIgnoreCase(
								term, pageable);
				case "reservationCode" ->
						reservationRepository.findByReservationCodeContainingIgnoreCase(
								term, pageable);
				case "channelName" -> searchByChannelName(term, pageable);
				default -> throw new BusinessException("검색 항목이 올바르지 않습니다.");
			};
		}

		return toPageResponse(reservations);
	}

	/** 수동 노쇼 처리 대상 목록. 한 페이지에 20건입니다. */
	public PageResponse<ReservationResponse> getNoShowCandidates(int page) {
		if (page < 0) {
			throw new BusinessException("페이지 번호는 0 이상이어야 합니다.");
		}

		PageRequest pageable = PageRequest.of(
				page,
				PageResponse.DEFAULT_PAGE_SIZE,
				Sort.by(Sort.Direction.ASC, "checkInDate")
						.and(Sort.by(Sort.Direction.ASC, "id"))
		);

		Page<Reservation> reservations =
				reservationRepository.findByStatusAndCheckInDateLessThanEqual(
						ReservationStatus.CONFIRMED,
						LocalDate.now(),
						pageable
				);

		return toPageResponse(reservations);
	}

	/** 페이지에 있는 객실·채널을 모아서 조회하고 응답으로 변환합니다. */
	private PageResponse<ReservationResponse> toPageResponse(
			Page<Reservation> reservations
	) {
		List<Long> roomIds = reservations.getContent().stream()
				.map(Reservation::getRoomId)
				.distinct()
				.toList();

		List<Long> channelIds = reservations.getContent().stream()
				.map(Reservation::getChannelId)
				.filter(Objects::nonNull)
				.distinct()
				.toList();

		Map<Long, String> rooms = roomRepository.findAllById(roomIds).stream()
				.collect(Collectors.toMap(Room::getId, Room::getRoomNumber));

		Map<Long, String> channels = channelRepository.findAllById(channelIds).stream()
				.collect(Collectors.toMap(Channel::getId, Channel::getChannelName));

		return PageResponse.from(reservations.map(reservation ->
				ReservationResponse.from(
						reservation,
						rooms.getOrDefault(
								reservation.getRoomId(),
								"객실 #" + reservation.getRoomId()
						),
						reservation.getChannelId() == null
								? "직접 예약"
								: channels.getOrDefault(
								reservation.getChannelId(),
								"채널 #" + reservation.getChannelId()
						)
				)
		));
	}

	/** 채널 이름에 맞는 ID를 찾아 예약을 검색합니다. */
	private Page<Reservation> searchByChannelName(
			String term, Pageable pageable
	) {
		String lowerTerm = term.toLowerCase(Locale.ROOT);

		List<Long> ids = channelRepository.findAll().stream()
				.filter(channel -> channel.getChannelName()
						.toLowerCase(Locale.ROOT).contains(lowerTerm))
				.map(Channel::getId)
				.toList();

		boolean direct = "직접 예약".contains(term);

		if (direct && ids.isEmpty()) {
			return reservationRepository.findByChannelIdIsNull(pageable);
		}
		if (direct) {
			return reservationRepository.findByChannelIdInOrChannelIdIsNull(
					ids, pageable);
		}
		if (ids.isEmpty()) {
			return Page.empty(pageable);
		}
		return reservationRepository.findByChannelIdIn(ids, pageable);
	}

	/** 예약 취소. 사유는 선택 입력이며 최대 255자입니다. */
	@Transactional
	public ReservationResponse cancel(Long id, String reason) {
		if (reason != null && reason.length() > 255) {
			throw new BusinessException(
					"cancelReason: 취소 사유는 255자 이내로 입력해 주세요.");
		}

		String cancelReason = reason == null ? null : reason.trim();

		if (cancelReason != null && cancelReason.isEmpty()) {
			cancelReason = null;
		}

		Reservation reservation = changeStatus(
				id, ReservationStatus.CANCELLED);

		reservation.recordCancelReason(cancelReason);
		reservationRepository.flush();

		return getDetail(id);
	}

	/** 프런트 직원이 선택한 예약을 수동으로 노쇼 처리합니다. */
	@Transactional
	public ReservationResponse markNoShow(Long id) {
		Reservation reservation = changeStatus(
				id, ReservationStatus.NOSHOW);

		reservationRepository.flush();

		return getDetail(reservation.getId());
	}

	/**
	 * 예약 상태 변경의 공통 진입점.
	 * 취소는 환불까지 같은 트랜잭션에서 처리합니다.
	 */
	@Transactional
	public Reservation changeStatus(Long id, ReservationStatus newStatus) {
		Reservation reservation = reservationRepository.findById(id)
				.orElseThrow(() ->
						new BusinessException("예약을 찾을 수 없습니다."));

		if (newStatus == null) {
			throw new BusinessException("변경할 예약 상태를 선택해 주세요.");
		}

		LocalDate today = LocalDate.now();

		if (newStatus == ReservationStatus.CANCELLED) {
			if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
				throw new BusinessException(
						"예약 확정 상태에서만 취소할 수 있습니다.");
			}

			if (!reservation.getCheckInDate().isAfter(today)) {
				throw new BusinessException(
						"체크인 당일 또는 지난 예약은 취소할 수 없습니다.");
			}
		}

		if (newStatus == ReservationStatus.NOSHOW) {
			if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
				throw new BusinessException(
						"예약 확정 상태에서만 노쇼 처리할 수 있습니다.");
			}

			if (reservation.getCheckInDate().isAfter(today)) {
				throw new BusinessException(
						"체크인 예정일이 오늘이거나 지난 예약만 노쇼 처리할 수 있습니다.");
			}
		}

		reservation.changeStatus(newStatus);

		if (newStatus == ReservationStatus.CANCELLED) {
			paymentService.refundForCancellation(id);
		}

		// CHECKED_IN: 키 번호·객실 상태 연결은 이후 구현
		// CHECKED_OUT: 미정산 추가 요금 확인·객실 상태 연결은 이후 구현

		return reservation;
	}

	/** 예: R20261116-3F9A1C7E */
	private String newReservationCode(LocalDate checkInDate) {
		String random = UUID.randomUUID().toString()
				.replace("-", "")
				.substring(0, 8)
				.toUpperCase(Locale.ROOT);

		return "R" + checkInDate.format(CODE_DATE) + "-" + random;
	}
}