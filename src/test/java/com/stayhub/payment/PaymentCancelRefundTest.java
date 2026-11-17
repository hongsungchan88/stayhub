package com.stayhub.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stayhub.channel.repository.ChannelRepository;
import com.stayhub.common.BusinessException;
import com.stayhub.common.PropertyScope;
import com.stayhub.payment.dto.CancelRefundResponse;
import com.stayhub.payment.dto.PaymentCreateRequest;
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
 * 예약 취소 환불 미리보기·일괄 환불 테스트 (RSV-002 연동).
 *
 * <p>환불 요청일은 오늘이라, 체크인 날짜를 오늘 기준으로 잡아 전액·부분을 가릅니다.
 */
@SpringBootTest
@Transactional
class PaymentCancelRefundTest {

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
		channelId = ChannelFixture.save(channelRepository, "10.00", true).getId();
		roomId = roomRepository.save(Room.create(
				PropertyScope.DEFAULT_PROPERTY_ID, "CANCELREFUND-101", "STANDARD", new BigDecimal("100000.00"), 2)).getId();
	}

	@Test
	@DisplayName("미리보기: 체크인 3일 넘게 남았으면 FULL·전액, 결제 상태는 그대로")
	void previewFull() {
		Long reservationId = reservation(TODAY.plusDays(10));
		Long paymentId = pay(reservationId, "100000.00");

		CancelRefundResponse preview = paymentService.previewCancelRefund(reservationId);

		assertThat(preview.reservationId()).isEqualTo(reservationId);
		assertThat(preview.checkInDate()).isEqualTo(TODAY.plusDays(10));
		assertThat(preview.refundRule()).isEqualTo(PaymentRefundRule.FULL);
		assertThat(preview.paymentTotal()).isEqualByComparingTo("100000.00");
		assertThat(preview.refundTotal()).isEqualByComparingTo("100000.00");
		assertThat(preview.payments()).singleElement()
				.satisfies(item -> assertThat(item.paymentId()).isEqualTo(paymentId));
		assertThat(paymentService.getDetail(paymentId).paymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
	}

	@Test
	@DisplayName("미리보기: 체크인 3일 이내면 PARTIAL·50%")
	void previewPartial() {
		Long reservationId = reservation(TODAY.plusDays(2));
		pay(reservationId, "100000.00");

		CancelRefundResponse preview = paymentService.previewCancelRefund(reservationId);

		assertThat(preview.refundRule()).isEqualTo(PaymentRefundRule.PARTIAL);
		assertThat(preview.paymentTotal()).isEqualByComparingTo("100000.00");
		assertThat(preview.refundTotal()).isEqualByComparingTo("50000.00");
	}

	@Test
	@DisplayName("미리보기: 결제가 없으면 NONE·0 (예외 아님)")
	void previewNoPayment() {
		Long reservationId = reservation(TODAY.plusDays(10));

		CancelRefundResponse preview = paymentService.previewCancelRefund(reservationId);

		assertThat(preview.refundRule()).isEqualTo(PaymentRefundRule.NONE);
		assertThat(preview.paymentTotal()).isEqualByComparingTo("0");
		assertThat(preview.refundTotal()).isEqualByComparingTo("0");
		assertThat(preview.payments()).isEmpty();
	}

	@Test
	@DisplayName("미리보기: 결제 완료가 아닌 결제는 빼고, 2건이면 합계를 낸다")
	void previewExcludesNonCompletedAndSums() {
		Long reservationId = reservation(TODAY.plusDays(10));
		pay(reservationId, "100000.00");
		pay(reservationId, "30000.00");
		Long requestedId = pay(reservationId, "70000.00");
		paymentService.requestRefund(requestedId);

		CancelRefundResponse preview = paymentService.previewCancelRefund(reservationId);

		assertThat(preview.payments()).hasSize(2)
				.noneMatch(item -> item.paymentId().equals(requestedId));
		assertThat(preview.paymentTotal()).isEqualByComparingTo("130000.00");
		assertThat(preview.refundTotal()).isEqualByComparingTo("130000.00");
	}

	@Test
	@DisplayName("일괄 환불: 3일 넘게 남았으면 REFUNDED·전액·요청일 오늘, 미리보기와 금액이 같다")
	void refundFull() {
		Long reservationId = reservation(TODAY.plusDays(10));
		Long paymentId = pay(reservationId, "100000.00");
		CancelRefundResponse preview = paymentService.previewCancelRefund(reservationId);

		CancelRefundResponse result = paymentService.refundForCancellation(reservationId);

		assertThat(result).isEqualTo(preview);
		PaymentResponse payment = paymentService.getDetail(paymentId);
		assertThat(payment.paymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
		assertThat(payment.refundRuleApplied()).isEqualTo(PaymentRefundRule.FULL);
		assertThat(payment.refundAmount()).isEqualByComparingTo("100000.00");
		assertThat(payment.refundRequestDate()).isEqualTo(TODAY);
	}

	@Test
	@DisplayName("일괄 환불: 3일 이내면 결제 2건 모두 PARTIALLY_REFUNDED·50%, 미리보기와 금액이 같다")
	void refundPartialTwoPayments() {
		Long reservationId = reservation(TODAY.plusDays(2));
		Long first = pay(reservationId, "100000.00");
		Long second = pay(reservationId, "30001.00");
		CancelRefundResponse preview = paymentService.previewCancelRefund(reservationId);

		CancelRefundResponse result = paymentService.refundForCancellation(reservationId);

		assertThat(result).isEqualTo(preview);
		assertThat(result.refundRule()).isEqualTo(PaymentRefundRule.PARTIAL);
		assertThat(result.refundTotal()).isEqualByComparingTo("65000.50");
		assertThat(paymentService.getDetail(first).paymentStatus()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
		assertThat(paymentService.getDetail(first).refundAmount()).isEqualByComparingTo("50000.00");
		assertThat(paymentService.getDetail(second).paymentStatus()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
		assertThat(paymentService.getDetail(second).refundAmount()).isEqualByComparingTo("15000.50");
		assertThat(paymentService.getDetail(second).refundRequestDate()).isEqualTo(TODAY);
	}

	@Test
	@DisplayName("일괄 환불: 결제가 없으면 NONE·0 (예외 아님)")
	void refundNoPayment() {
		Long reservationId = reservation(TODAY.plusDays(10));

		CancelRefundResponse result = paymentService.refundForCancellation(reservationId);

		assertThat(result.refundRule()).isEqualTo(PaymentRefundRule.NONE);
		assertThat(result.refundTotal()).isEqualByComparingTo("0");
		assertThat(result.payments()).isEmpty();
	}

	@Test
	@DisplayName("일괄 환불: 결제 완료가 아닌 결제와 다른 예약의 결제는 바뀌지 않는다")
	void refundLeavesOtherPaymentsAlone() {
		Long reservationId = reservation(TODAY.plusDays(10));
		Long completedId = pay(reservationId, "100000.00");
		Long requestedId = pay(reservationId, "70000.00");
		paymentService.requestRefund(requestedId);
		Long otherId = pay(reservation(TODAY.plusDays(20)), "100000.00");

		CancelRefundResponse result = paymentService.refundForCancellation(reservationId);

		assertThat(result.payments()).singleElement()
				.satisfies(item -> assertThat(item.paymentId()).isEqualTo(completedId));
		PaymentResponse requested = paymentService.getDetail(requestedId);
		assertThat(requested.paymentStatus()).isEqualTo(PaymentStatus.REFUND_REQUESTED);
		assertThat(requested.refundAmount()).isNull();
		PaymentResponse other = paymentService.getDetail(otherId);
		assertThat(other.paymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
		assertThat(other.refundRequestDate()).isNull();
	}

	@Test
	@DisplayName("없는 예약이면 미리보기·일괄 환불 모두 반려")
	void missingReservationRejected() {
		assertThatThrownBy(() -> paymentService.previewCancelRefund(-1L))
				.isInstanceOf(BusinessException.class)
				.hasMessage("예약을 찾을 수 없습니다.");
		assertThatThrownBy(() -> paymentService.refundForCancellation(-1L))
				.isInstanceOf(BusinessException.class)
				.hasMessage("예약을 찾을 수 없습니다.");
	}

	private Long reservation(LocalDate checkIn) {
		return reservationService.create(
				null, "취소환불테스트", "010-0000-0000", roomId, null, checkIn, checkIn.plusDays(1), null).getId();
	}

	private Long pay(Long reservationId, String amount) {
		return paymentService.create(new PaymentCreateRequest(
				reservationId, channelId, "사전 결제", new BigDecimal(amount), null)).id();
	}
}
