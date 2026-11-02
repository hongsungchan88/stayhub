package com.stayhub.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stayhub.common.BusinessException;
import com.stayhub.common.PropertyScope;
import com.stayhub.reservation.entity.Reservation;
import com.stayhub.reservation.entity.ReservationStatus;
import com.stayhub.reservation.service.ReservationService;
import com.stayhub.room.entity.Room;
import com.stayhub.room.repository.RoomRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 예약 생성(중복 방지)·상태 전이 테스트.
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>실제 DB 에 테스트용 객실을 하나 만들고 날짜 겹침·상태 전이 규칙을 확인합니다.
 * {@code @Transactional} 이라 끝나면 롤백됩니다. 동시 요청은 이 방식으로 확인할 수 없어서
 * {@link ReservationConcurrencyTest} 에서 따로 확인합니다.
 */
@SpringBootTest
@Transactional
class ReservationServiceTest {

	private static final LocalDate D1 = LocalDate.of(2026, 12, 1);

	@Autowired
	private ReservationService reservationService;

	@Autowired
	private RoomRepository roomRepository;

	private Long roomId;

	@BeforeEach
	void setUp() {
		roomId = roomRepository.save(Room.create(
				PropertyScope.DEFAULT_PROPERTY_ID, "RSVTEST-101", "STANDARD", new BigDecimal("100000.00"), 2)).getId();
	}

	@Test
	@DisplayName("만들면 CONFIRMED 로 시작하고 예약 번호가 붙는다")
	void createStartsConfirmed() {
		Reservation reservation = reserve(D1, D1.plusDays(2));

		assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
		assertThat(reservation.getReservationCode()).startsWith("R20261201-");
		assertThat(reservation.getGuestCount()).isEqualTo(1); // 비우면 1
	}

	@Test
	@DisplayName("같은 객실에 날짜가 겹치면 거부한다")
	void overlappingDatesRejected() {
		reserve(D1, D1.plusDays(3)); // 12/1 ~ 12/4

		assertThatThrownBy(() -> reserve(D1.plusDays(2), D1.plusDays(5))) // 12/3 ~ 12/6
				.isInstanceOf(BusinessException.class)
				.hasMessage("선택한 날짜에 이미 예약된 객실입니다.");
		assertThatThrownBy(() -> reserve(D1.minusDays(1), D1.plusDays(5))) // 기존 예약을 통째로 감쌈
				.isInstanceOf(BusinessException.class);
	}

	@Test
	@DisplayName("앞 예약의 체크아웃 날에 체크인하는 것은 허용한다")
	void checkInOnPreviousCheckOutDayAllowed() {
		reserve(D1, D1.plusDays(2)); // 12/1 ~ 12/3

		Reservation next = reserve(D1.plusDays(2), D1.plusDays(4)); // 12/3 ~ 12/5
		assertThat(next.getId()).isNotNull();
	}

	@Test
	@DisplayName("취소·노쇼된 예약과는 날짜가 겹쳐도 허용한다")
	void releasedReservationsDoNotBlock() {
		Reservation cancelled = reserve(D1, D1.plusDays(2));
		reservationService.changeStatus(cancelled.getId(), ReservationStatus.CANCELLED);
		Reservation noShow = reserve(D1, D1.plusDays(2));
		reservationService.changeStatus(noShow.getId(), ReservationStatus.NOSHOW);

		assertThat(reserve(D1, D1.plusDays(2)).getId()).isNotNull();
	}

	@Test
	@DisplayName("체크아웃이 체크인과 같거나 앞이면 거부한다")
	void checkOutMustBeAfterCheckIn() {
		assertThatThrownBy(() -> reserve(D1, D1))
				.isInstanceOf(BusinessException.class)
				.hasMessage("체크아웃 날짜는 체크인 날짜보다 뒤여야 합니다.");
		assertThatThrownBy(() -> reserve(D1, D1.minusDays(1)))
				.isInstanceOf(BusinessException.class);
	}

	@Test
	@DisplayName("없는 객실이면 거부한다")
	void unknownRoomRejected() {
		assertThatThrownBy(() -> reservationService.create(
				null, "게스트", "010-0000-0000", 99999999L, null, D1, D1.plusDays(1), null))
				.isInstanceOf(BusinessException.class)
				.hasMessage("객실을 찾을 수 없습니다.");
	}

	@Test
	@DisplayName("허용된 전이는 전부 성공한다 — CONFIRMED→CANCELLED/NOSHOW/CHECKED_IN, CHECKED_IN→CHECKED_OUT")
	void allowedTransitions() {
		for (ReservationStatus next : EnumSet.of(
				ReservationStatus.CANCELLED, ReservationStatus.NOSHOW, ReservationStatus.CHECKED_IN)) {
			Reservation reservation = reserve(D1.plusMonths(next.ordinal()), D1.plusMonths(next.ordinal()).plusDays(1));
			assertThat(reservationService.changeStatus(reservation.getId(), next).getStatus()).isEqualTo(next);
		}

		Reservation stay = reserve(D1.plusYears(1), D1.plusYears(1).plusDays(1));
		reservationService.changeStatus(stay.getId(), ReservationStatus.CHECKED_IN);
		assertThat(reservationService.changeStatus(stay.getId(), ReservationStatus.CHECKED_OUT).getStatus())
				.isEqualTo(ReservationStatus.CHECKED_OUT);
	}

	@Test
	@DisplayName("허용 안 된 전이는 거부한다 — 취소 되살리기, 체크인 건너뛴 체크아웃, 투숙 중 취소, 체크아웃 후 체크인")
	void disallowedTransitionsRejected() {
		Reservation cancelled = reserve(D1, D1.plusDays(1));
		reservationService.changeStatus(cancelled.getId(), ReservationStatus.CANCELLED);
		assertRejected(cancelled, ReservationStatus.CONFIRMED);

		Reservation confirmed = reserve(D1.plusDays(1), D1.plusDays(2));
		assertRejected(confirmed, ReservationStatus.CHECKED_OUT);

		Reservation checkedIn = reserve(D1.plusDays(2), D1.plusDays(3));
		reservationService.changeStatus(checkedIn.getId(), ReservationStatus.CHECKED_IN);
		assertRejected(checkedIn, ReservationStatus.CANCELLED);

		reservationService.changeStatus(checkedIn.getId(), ReservationStatus.CHECKED_OUT);
		assertRejected(checkedIn, ReservationStatus.CHECKED_IN);
	}

	@Test
	@DisplayName("전이 규칙 전체표 — 허용은 정확히 4가지")
	void transitionTable() {
		Set<String> allowed = new java.util.TreeSet<>();
		for (ReservationStatus from : ReservationStatus.values()) {
			for (ReservationStatus to : ReservationStatus.values()) {
				if (from.canChangeTo(to)) {
					allowed.add(from + "->" + to);
				}
			}
		}
		assertThat(allowed).containsExactlyInAnyOrder(
				"CONFIRMED->CANCELLED", "CONFIRMED->NOSHOW", "CONFIRMED->CHECKED_IN", "CHECKED_IN->CHECKED_OUT");
	}

	private void assertRejected(Reservation reservation, ReservationStatus next) {
		ReservationStatus before = reservation.getStatus();
		assertThatThrownBy(() -> reservationService.changeStatus(reservation.getId(), next))
				.isInstanceOf(BusinessException.class)
				.hasMessage("예약 상태를 " + before + "에서 " + next + "(으)로 바꿀 수 없습니다.");
		assertThat(reservation.getStatus()).isEqualTo(before);
	}

	private Reservation reserve(LocalDate checkIn, LocalDate checkOut) {
		return reservationService.create(null, "테스트게스트", "010-0000-0000", roomId, null, checkIn, checkOut, null);
	}
}
