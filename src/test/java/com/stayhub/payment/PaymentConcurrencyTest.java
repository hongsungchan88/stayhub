package com.stayhub.payment;

import static org.assertj.core.api.Assertions.assertThat;

import com.stayhub.channel.repository.ChannelRepository;
import com.stayhub.common.PropertyScope;
import com.stayhub.payment.dto.PaymentCreateRequest;
import com.stayhub.payment.dto.PaymentRefundRejectRequest;
import com.stayhub.payment.dto.PaymentResponse;
import com.stayhub.payment.entity.Payment;
import com.stayhub.payment.entity.PaymentStatus;
import com.stayhub.payment.repository.PaymentRepository;
import com.stayhub.payment.service.PaymentService;
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
import java.util.function.Supplier;
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
 * 같은 결제의 상태를 동시에 바꾸면 하나만 반영되는지 (정책정의서 40행).
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>스레드 2개가 각자 트랜잭션에서 같은 환불 요청 결제를 읽고, {@link CyclicBarrier} 로 서로를 기다린 뒤
 * 한쪽은 환불, 한쪽은 환불 거절을 합니다. 둘 다 REFUND_REQUESTED 를 본 상태라 상태 전이 검사는 모두 통과합니다.
 * 그래도 저장할 때 결제 version 이 충돌해 한쪽은 {@link ObjectOptimisticLockingFailureException}(409)으로 롤백돼야 합니다.
 *
 * <p>서비스가 안에서 flush 하므로 장벽은 서비스를 부른 뒤가 아니라 <b>읽은 뒤·부르기 전</b>에 둡니다.
 * 뒤에 두면 먼저 UPDATE 한 쪽의 행 잠금을 다른 쪽이 기다리느라 장벽에 도착하지 못합니다.
 *
 * <p>{@code @Transactional} 을 붙이지 않는 이유는 {@code ReservationConcurrencyTest} 와 같습니다.
 * 만든 데이터는 {@link #cleanUp} 에서 지웁니다.
 */
@SpringBootTest
class PaymentConcurrencyTest {

	@Autowired
	private PaymentService paymentService;

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private ReservationService reservationService;

	@Autowired
	private ReservationRepository reservationRepository;

	@Autowired
	private RoomRepository roomRepository;

	@Autowired
	private ChannelRepository channelRepository;

	@Autowired
	private PlatformTransactionManager transactionManager;

	private Long channelId;
	private Long roomId;
	private Long reservationId;
	private Long paymentId;

	@BeforeEach
	void setUp() {
		channelId = ChannelFixture.save(channelRepository, "10.00", true).getId();
		roomId = roomRepository.save(Room.create(
				PropertyScope.DEFAULT_PROPERTY_ID, "PAYTEST-CC1", "STANDARD", new BigDecimal("100000.00"), 2)).getId();
		LocalDate checkIn = LocalDate.now().plusDays(10); // 체크인 3일 전보다 앞이라 환불하면 전액(REFUNDED)
		reservationId = reservationService.create(
				null, "동시환불", "010-0000-0000", roomId, null, checkIn, checkIn.plusDays(1), null).getId();
		paymentId = paymentService.create(new PaymentCreateRequest(
				reservationId, channelId, "사전 결제", new BigDecimal("100000.00"), null)).id();
		paymentService.requestRefund(paymentId);
	}

	@AfterEach
	void cleanUp() {
		// setUp 이 중간에 실패하면 뒤쪽 ID 는 null 이다
		if (paymentId != null) {
			paymentRepository.deleteById(paymentId);
		}
		if (reservationId != null) {
			reservationRepository.deleteById(reservationId);
		}
		if (roomId != null) {
			roomRepository.deleteById(roomId);
		}
		if (channelId != null) {
			channelRepository.deleteById(channelId);
		}
	}

	@Test
	@DisplayName("환불과 환불 거절이 동시에 들어오면, 하나만 반영되고 나머지는 낙관적 락 예외(409)")
	void onlyOneOfConcurrentRefundAndRejectSucceeds() throws Exception {
		Long versionBefore = findPayment().getVersion();
		CyclicBarrier bothRead = new CyclicBarrier(2);
		TransactionTemplate tx = new TransactionTemplate(transactionManager);
		ExecutorService pool = Executors.newFixedThreadPool(2);

		List<Supplier<PaymentResponse>> changes = List.of(
				() -> paymentService.refund(paymentId),
				() -> paymentService.rejectRefund(paymentId, new PaymentRefundRejectRequest("규정 외 요청")));
		List<Future<PaymentStatus>> results = new ArrayList<>();
		for (Supplier<PaymentResponse> change : changes) {
			results.add(pool.submit(() -> tx.execute(status -> {
				findPayment(); // 여기서 읽은 엔티티를 같은 트랜잭션의 서비스가 그대로 쓴다(1차 캐시)
				await(bothRead); // 둘 다 같은 version 을 읽은 상태에서 저장으로 넘어간다
				return change.get().paymentStatus();
			})));
		}
		pool.shutdown();

		List<PaymentStatus> succeeded = new ArrayList<>();
		List<Throwable> failures = new ArrayList<>();
		for (Future<PaymentStatus> result : results) {
			try {
				succeeded.add(result.get(30, TimeUnit.SECONDS));
			} catch (ExecutionException e) {
				failures.add(e.getCause());
			}
		}

		assertThat(succeeded).hasSize(1);
		assertThat(failures).singleElement().isInstanceOf(ObjectOptimisticLockingFailureException.class);

		Payment saved = findPayment();
		assertThat(saved.getPaymentStatus()).isEqualTo(succeeded.get(0)); // 성공한 쪽 값이 남는다
		assertThat(saved.getVersion()).isEqualTo(versionBefore + 1);
		// 실패한 쪽이 채운 값이 섞여 남지 않는다
		if (saved.getPaymentStatus() == PaymentStatus.REFUNDED) {
			assertThat(saved.getRefundAmount()).isEqualByComparingTo("100000.00");
			assertThat(saved.getErrorNote()).isNull();
		} else {
			assertThat(saved.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
			assertThat(saved.getRefundAmount()).isNull();
			assertThat(saved.getErrorNote()).isEqualTo("규정 외 요청");
		}
	}

	private Payment findPayment() {
		return paymentRepository.findById(paymentId).orElseThrow();
	}

	private static void await(CyclicBarrier barrier) {
		try {
			barrier.await(10, TimeUnit.SECONDS);
		} catch (Exception e) {
			throw new IllegalStateException("다른 스레드를 기다리다 실패", e);
		}
	}
}
