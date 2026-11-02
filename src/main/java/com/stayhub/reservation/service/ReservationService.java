package com.stayhub.reservation.service;

import com.stayhub.common.BusinessException;
import com.stayhub.reservation.entity.Reservation;
import com.stayhub.reservation.entity.ReservationStatus;
import com.stayhub.reservation.repository.ReservationRepository;
import com.stayhub.room.repository.RoomRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 예약 생성(중복 방지)과 상태 전이 — 총괄이 먼저 올리는 틀 (정책정의서 39·40행).
 *
 * <p><b>이 파일이 하는 일</b>
 * <ul>
 *   <li>{@link #create} — 같은 객실에 날짜가 겹치는 예약을 막습니다. 동시에 들어온 두 요청도 하나만 성공합니다</li>
 *   <li>{@link #changeStatus} — {@link ReservationStatus#canChangeTo} 규칙대로만 상태를 바꿉니다</li>
 * </ul>
 *
 * <p><b>동시 예약을 막는 방법 (정책 40행 — 낙관적 락 한 가지)</b>
 * <br>겹침 검사만으로는 부족합니다. 두 요청이 거의 동시에 오면 둘 다 "겹치는 예약 없음" 을 보고 둘 다 저장할 수 있습니다.
 * 그래서 예약할 객실을 {@code RoomRepository.findForReservationById}({@code OPTIMISTIC_FORCE_INCREMENT})로 읽어,
 * 트랜잭션이 끝날 때 객실의 version 을 하나 올립니다. 같은 객실을 같은 version 으로 읽은 두 요청 중
 * 나중에 끝나는 쪽은 version 이 이미 바뀌어 {@code ObjectOptimisticLockingFailureException} 으로 롤백되고,
 * {@code GlobalExceptionHandler} 가 409 로 응답합니다. 비관적 락(SELECT ... FOR UPDATE)은 쓰지 않습니다.
 *
 * <p><b>담당자(B)에게</b>
 * <br>컨트롤러·요청/응답 DTO·목록·검색은 이 위에 만드세요. 예약을 만드는 코드는 어디서든 반드시 {@link #create} 를 거칩니다.
 * {@code reservationRepository.save(Reservation.create(...))} 를 직접 쓰면 중복 방지가 통째로 빠집니다.
 */
@Service
@Transactional(readOnly = true)
public class ReservationService {

	private static final DateTimeFormatter CODE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

	private final ReservationRepository reservationRepository;
	private final RoomRepository roomRepository;

	public ReservationService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
		this.reservationRepository = reservationRepository;
		this.roomRepository = roomRepository;
	}

	/**
	 * 예약 생성 (RSV-001).
	 *
	 * @param userId    회원 예약이면 사용자 ID, 비회원이면 null
	 * @param channelId 예약 채널 ID, 직접 예약이면 null
	 * @param guestCount 비어 있으면 1
	 * @throws BusinessException 날짜가 잘못됐거나, 객실이 없거나, 날짜가 겹칠 때
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
		if (!checkOutDate.isAfter(checkInDate)) {
			throw new BusinessException("체크아웃 날짜는 체크인 날짜보다 뒤여야 합니다.");
		}
		// 객실 version 을 이 트랜잭션 끝에 올리도록 표시합니다. 겹침 검사보다 먼저 해야 동시 요청을 잡습니다.
		roomRepository.findForReservationById(roomId)
				.orElseThrow(() -> new BusinessException("객실을 찾을 수 없습니다."));

		if (reservationRepository.existsOverlap(roomId, checkInDate, checkOutDate, ReservationStatus.RELEASED)) {
			throw new BusinessException("선택한 날짜에 이미 예약된 객실입니다.");
		}
		// B: 인원(guest_count ≤ rooms.max_occupancy)·채널 존재 확인이 필요하면 여기에 추가

		Reservation reservation = Reservation.create(
				newReservationCode(checkInDate), userId, guestName, guestPhone,
				roomId, channelId, checkInDate, checkOutDate, guestCount);
		return reservationRepository.save(reservation);
	}

	/**
	 * 상태 변경. 허용 규칙은 {@link ReservationStatus#canChangeTo}.
	 *
	 * @throws BusinessException 없는 예약이거나 허용되지 않는 변경일 때
	 */
	@Transactional
	public Reservation changeStatus(Long id, ReservationStatus newStatus) {
		Reservation reservation = reservationRepository.findById(id)
				.orElseThrow(() -> new BusinessException("예약을 찾을 수 없습니다."));
		reservation.changeStatus(newStatus);

		// B: 아래는 담당자가 채울 연동입니다. 상태 변경과 같은 트랜잭션에서 처리하세요.
		//  - CANCELLED  : 취소 수수료·환불 금액 산출, 취소 불가 기간 거절, cancel_reason 저장 (RSV-002, PAY-001)
		//  - NOSHOW     : 체크인 예정일 당일 미입실 자동 전환, 객실 재오픈 (RSV-003)
		//  - CHECKED_IN : 키 번호 매핑, 객실 상태 → OCCUPIED (RSV-004, RoomService.changeStatus)
		//  - CHECKED_OUT: 미정산 추가 요금이 있으면 차단, 객실 상태 → CLEANING_NEEDED (RSV-004, PAY-002)
		return reservation;
	}

	/**
	 * 예약 번호. 예) R20261102-3F9A1C7E. UNIQUE 컬럼이라 혹시 겹치면 저장이 실패합니다.
	 * 형식을 바꾸려면 이 메서드만 고치면 됩니다(B).
	 */
	private String newReservationCode(LocalDate checkInDate) {
		String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
		return "R" + checkInDate.format(CODE_DATE) + "-" + random;
	}
}
