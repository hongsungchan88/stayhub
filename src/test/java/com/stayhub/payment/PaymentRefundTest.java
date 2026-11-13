package com.stayhub.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stayhub.channel.repository.ChannelRepository;
import com.stayhub.common.BusinessException;
import com.stayhub.common.PropertyScope;
import com.stayhub.payment.dto.PaymentCreateRequest;
import com.stayhub.payment.dto.PaymentRefundRejectRequest;
import com.stayhub.payment.dto.PaymentResponse;
import com.stayhub.payment.entity.PaymentRefundRule;
import com.stayhub.payment.entity.PaymentStatus;
import com.stayhub.payment.service.PaymentService;
import com.stayhub.reservation.service.ReservationService;
import com.stayhub.room.entity.Room;
import com.stayhub.room.repository.RoomRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 환불 요청·처리·거절 테스트 (PAY-001).
 *
 * <p>환불 요청일은 서버가 오늘 날짜로 넣기 때문에, 체크인 날짜를 오늘 기준으로 잡아 전액·부분을 가릅니다.
 * {@code @Transactional} 이라 테스트가 끝나면 넣은 객실·예약·결제가 전부 되돌려집니다.
 */
@SpringBootTest
@Transactional
class PaymentRefundTest {

	private static final LocalDate TODAY = LocalDate.now();

	@Autowired
	private PaymentService paymentService;

	@Autowired
	private ReservationService reservationService;

	@Autowired
	private RoomRepository roomRepository;

	@Autowired
	private ChannelRepository channelRepository;

	private Long roomId;
	private Long channelId;

	@BeforeEach
	void setUp() {
		// PAY-003: 결제 등록 때 채널 수수료율을 읽으므로 실제 채널이 있어야 합니다.
		channelId = ChannelFixture.save(channelRepository, "10.00", true).getId();
		roomId = roomRepository.save(Room.create(
				PropertyScope.DEFAULT_PROPERTY_ID, "REFUNDTEST-101", "STANDARD", new BigDecimal("100000.00"), 2)).getId();
	}

	@Test
	@DisplayName("환불 요청하면 REFUND_REQUESTED 가 되고 요청일이 오늘로 저장된다")
	void requestRefundSavesDate() {
		Long paymentId = paidFor(TODAY.plusDays(10));

		PaymentResponse requested = paymentService.requestRefund(paymentId);

		assertThat(requested.paymentStatus()).isEqualTo(PaymentStatus.REFUND_REQUESTED);
		assertThat(requested.refundRequestDate()).isEqualTo(TODAY);
		assertThat(requested.refundAmount()).isNull();
	}

	@Test
	@DisplayName("체크인 3일 전까지 요청했으면 전액 환불")
	void fullRefundWhenThreeDaysBefore() {
		Long paymentId = paidFor(TODAY.plusDays(3)); // 오늘이 딱 3일 전 — 경계값

		paymentService.requestRefund(paymentId);
		PaymentResponse refunded = paymentService.refund(paymentId);

		assertThat(refunded.paymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
		assertThat(refunded.refundRuleApplied()).isEqualTo(PaymentRefundRule.FULL);
		assertThat(refunded.refundAmount()).isEqualByComparingTo("100000.00");
	}

	@Test
	@DisplayName("체크인 3일 전이 지나서 요청했으면 부분 환불 (임시 비율 50%)")
	void partialRefundAfterDeadline() {
		Long paymentId = paidFor(TODAY.plusDays(2));

		paymentService.requestRefund(paymentId);
		PaymentResponse refunded = paymentService.refund(paymentId);

		assertThat(refunded.paymentStatus()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
		assertThat(refunded.refundRuleApplied()).isEqualTo(PaymentRefundRule.PARTIAL);
		assertThat(refunded.refundAmount()).isEqualByComparingTo("50000.00");
	}

	@Test
	@DisplayName("환불 요청 없이 바로 환불하면 반려된다")
	void refundWithoutRequestRejected() {
		Long paymentId = paidFor(TODAY.plusDays(10));

		assertThatThrownBy(() -> paymentService.refund(paymentId))
				.isInstanceOf(BusinessException.class)
				.hasMessage("환불 요청 상태의 결제만 환불할 수 있습니다.");
	}

	@Test
	@DisplayName("이미 환불된 결제는 다시 환불 요청할 수 없다")
	void refundedPaymentCannotBeRequestedAgain() {
		Long paymentId = paidFor(TODAY.plusDays(10));
		paymentService.requestRefund(paymentId);
		paymentService.refund(paymentId);

		assertThatThrownBy(() -> paymentService.requestRefund(paymentId))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("결제 상태를 REFUNDED에서 REFUND_REQUESTED(으)로 바꿀 수 없습니다");
	}

	@Test
	@DisplayName("환불 거절하면 결제 완료로 돌아가고 규정 NONE, 사유가 남는다. 다시 요청할 수 있다")
	void rejectRefund() {
		Long paymentId = paidFor(TODAY.plusDays(10));
		paymentService.requestRefund(paymentId);

		PaymentResponse rejected = paymentService.rejectRefund(paymentId, new PaymentRefundRejectRequest("규정 외 요청"));

		assertThat(rejected.paymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
		assertThat(rejected.refundRuleApplied()).isEqualTo(PaymentRefundRule.NONE);
		assertThat(rejected.errorNote()).isEqualTo("규정 외 요청");
		assertThat(rejected.refundAmount()).isNull();

		PaymentResponse again = paymentService.requestRefund(paymentId);
		assertThat(again.paymentStatus()).isEqualTo(PaymentStatus.REFUND_REQUESTED);
		assertThat(again.refundRuleApplied()).isNull();
	}

	@Test
	@DisplayName("환불 요청 상태가 아니면 거절할 수 없다")
	void rejectWithoutRequestRejected() {
		Long paymentId = paidFor(TODAY.plusDays(10));

		assertThatThrownBy(() -> paymentService.rejectRefund(paymentId, new PaymentRefundRejectRequest("사유")))
				.isInstanceOf(BusinessException.class);
	}

	@Test
	@DisplayName("결제의 예약이 없으면 환불 처리를 반려한다")
	void refundWithoutReservationRejected() {
		Long paymentId = paymentService.create(new PaymentCreateRequest(
				-1L, channelId, "사전 결제", new BigDecimal("100000.00"), null)).id();
		paymentService.requestRefund(paymentId);

		assertThatThrownBy(() -> paymentService.refund(paymentId))
				.isInstanceOf(BusinessException.class)
				.hasMessage("결제의 예약을 찾을 수 없습니다.");
	}

	/** 체크인 날짜가 checkIn 인 예약을 만들고, 그 예약에 100,000원 결제를 등록해 결제 ID 를 돌려줍니다. */
	private Long paidFor(LocalDate checkIn) {
		Long reservationId = reservationService.create(
				null, "환불테스트", "010-0000-0000", roomId, null, checkIn, checkIn.plusDays(1), null).getId();
		return paymentService.create(new PaymentCreateRequest(
				reservationId, channelId, "사전 결제", new BigDecimal("100000.00"), null)).id();
	}
}
