package com.stayhub.reservation;

import static org.assertj.core.api.Assertions.assertThat;

import com.stayhub.common.PropertyScope;
import com.stayhub.reservation.dto.ReservationUpdateRequest;
import com.stayhub.reservation.entity.Reservation;
import com.stayhub.reservation.repository.ReservationRepository;
import com.stayhub.reservation.service.ReservationService;
import com.stayhub.room.entity.Room;
import com.stayhub.room.repository.RoomRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 같은 객실·겹치는 날짜로 동시에 예약하거나 예약을 변경하면 하나만 성공하는지 (정책정의서 40행).
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>스레드 2개가 각자 트랜잭션에서 {@code ReservationService.create}(또는 {@code update})를 부릅니다.
 * 둘 다 겹침 검사를 통과한 뒤 커밋 직전에 {@link CyclicBarrier} 로 서로를 기다리게 해서,
 * "둘 다 검사를 통과했다" 는 가장 위험한 순간을 매번 똑같이 만듭니다. 그러면 커밋할 때
 * 객실 version 이 충돌해 한쪽은 {@link ObjectOptimisticLockingFailureException}(409)으로 롤백돼야 합니다.
 *
 * <p><b>{@code @Transactional} 을 붙이지 않습니다.</b> 테스트 트랜잭션 안에서는 커밋이 일어나지 않아 충돌을 볼 수 없습니다.
 * 대신 만든 객실·예약을 {@link #cleanUp} 에서 직접 지웁니다. 테스트가 중간에 실패해도 지워집니다.
 */
@SpringBootTest
class ReservationConcurrencyTest {

	private static final LocalDate CHECK_IN = LocalDate.now().plusDays(30);

	@Autowired
	private ReservationService reservationService;

	@Autowired
	private ReservationRepository reservationRepository;

	@Autowired
	private RoomRepository roomRepository;

	@Autowired
	private PlatformTransactionManager transactionManager;

	private Long roomId;

	@BeforeEach
	void setUp() {
		roomId = roomRepository.save(Room.create(
				PropertyScope.DEFAULT_PROPERTY_ID, "RSVTEST-CC1", "STANDARD", new BigDecimal("100000.00"), 2)).getId();
	}

	@AfterEach
	void cleanUp() {
		reservationRepository.deleteAll(reservationsOfRoom());
		roomRepository.deleteById(roomId);
	}

	@Test
	@DisplayName("두 요청이 동시에 겹침 검사를 통과해도, 커밋은 하나만 성공하고 나머지는 낙관적 락 예외(409)")
	void onlyOneOfTwoConcurrentReservationsSucceeds() throws Exception {
		List<Throwable> failures = runTogether(() -> reserve(CHECK_IN), () -> reserve(CHECK_IN));

		assertThat(failures).singleElement().isInstanceOf(ObjectOptimisticLockingFailureException.class);
		assertThat(reservationsOfRoom()).hasSize(1); // 실패한 쪽의 예약은 롤백돼 남지 않는다
	}

	@Test
	@DisplayName("같은 객실의 두 예약을 겹치는 날짜로 동시에 변경해도, 커밋은 하나만 성공하고 나머지는 낙관적 락 예외(409)")
	void onlyOneOfTwoConcurrentUpdatesSucceeds() throws Exception {
		LocalDate otherCheckIn = CHECK_IN.plusDays(10);
		LocalDate target = CHECK_IN.plusDays(20);
		Long firstId = reserve(CHECK_IN);
		Long secondId = reserve(otherCheckIn);

		// target+1 하루가 겹친다
		List<Throwable> failures = runTogether(
				() -> reservationService.update(
						firstId, new ReservationUpdateRequest(roomId, target, target.plusDays(2))),
				() -> reservationService.update(
						secondId, new ReservationUpdateRequest(roomId, target.plusDays(1), target.plusDays(3))));

		assertThat(failures).singleElement().isInstanceOf(ObjectOptimisticLockingFailureException.class);
		// 한쪽만 옮겨지고, 실패한 쪽은 롤백돼 원래 날짜 그대로다
		assertThat(List.of(checkInOf(firstId), checkInOf(secondId))).isIn(
				List.of(target, otherCheckIn),
				List.of(CHECK_IN, target.plusDays(1)));
	}

	/** 두 작업을 각자 트랜잭션에서 실행하되 둘 다 끝난 뒤에 커밋하게 하고, 실패한 쪽의 예외를 돌려줍니다. */
	private List<Throwable> runTogether(Runnable first, Runnable second) throws Exception {
		CyclicBarrier bothChecked = new CyclicBarrier(2);
		TransactionTemplate tx = new TransactionTemplate(transactionManager);
		ExecutorService pool = Executors.newFixedThreadPool(2);

		List<Future<?>> results = new ArrayList<>();
		for (Runnable work : List.of(first, second)) {
			results.add(pool.submit(() -> tx.executeWithoutResult(status -> {
				work.run();
				await(bothChecked); // 둘 다 겹침 검사를 통과한 상태에서 커밋으로 넘어간다
			})));
		}
		pool.shutdown();

		List<Throwable> failures = new ArrayList<>();
		for (Future<?> result : results) {
			try {
				result.get(30, TimeUnit.SECONDS);
			} catch (ExecutionException e) {
				failures.add(e.getCause());
			}
		}
		return failures;
	}

	private Long reserve(LocalDate checkIn) {
		return reservationService.create(
				null, "동시게스트", "010-0000-0000", roomId, null, checkIn, checkIn.plusDays(1), null).getId();
	}

	private LocalDate checkInOf(Long reservationId) {
		return reservationRepository.findById(reservationId).orElseThrow().getCheckInDate();
	}

	private List<Reservation> reservationsOfRoom() {
		return reservationRepository.findAll().stream()
				.filter(reservation -> reservation.getRoomId().equals(roomId))
				.toList();
	}

	private static void await(CyclicBarrier barrier) {
		try {
			barrier.await(10, TimeUnit.SECONDS);
		} catch (Exception e) {
			throw new IllegalStateException("다른 스레드를 기다리다 실패", e);
		}
	}
}
