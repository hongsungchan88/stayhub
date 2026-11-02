package com.stayhub.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stayhub.common.BusinessException;
import com.stayhub.payment.entity.Payment;
import com.stayhub.payment.entity.PaymentStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 결제 상태 전이 테스트 (DB 없이 엔티티만).
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>{@link PaymentStatus#canChangeTo} 규칙과 {@code Payment.changeStatus} 가 그 규칙대로 막는지 확인합니다.
 * FAILED 처럼 지금 코드로는 만들 수 없는 상태는 {@code ReflectionTestUtils} 로 직접 넣습니다(TaxInvoiceServiceTest 와 같은 방식).
 */
class PaymentStatusTest {

	@Test
	@DisplayName("허용된 전이는 전부 성공한다 — 환불 요청, 환불 완료·부분 환불·환불 거절")
	void allowedTransitions() {
		Payment payment = newPayment();
		payment.changeStatus(PaymentStatus.REFUND_REQUESTED);
		assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.REFUND_REQUESTED);

		payment.changeStatus(PaymentStatus.COMPLETED); // 환불 거절
		assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);

		for (PaymentStatus done : List.of(PaymentStatus.REFUNDED, PaymentStatus.PARTIALLY_REFUNDED)) {
			Payment refunding = newPayment();
			refunding.changeStatus(PaymentStatus.REFUND_REQUESTED);
			refunding.changeStatus(done);
			assertThat(refunding.getPaymentStatus()).isEqualTo(done);
		}
	}

	@Test
	@DisplayName("막히는 전이 — 요청 없이 바로 환불, 실패 결제 되살리기, 환불 끝난 결제 다시 요청, 부분 환불 후 전액 환불")
	void disallowedTransitionsRejected() {
		assertRejected(PaymentStatus.COMPLETED, PaymentStatus.REFUNDED);
		assertRejected(PaymentStatus.FAILED, PaymentStatus.COMPLETED);
		assertRejected(PaymentStatus.REFUNDED, PaymentStatus.REFUND_REQUESTED);
		assertRejected(PaymentStatus.PARTIALLY_REFUNDED, PaymentStatus.REFUNDED);
	}

	@Test
	@DisplayName("전이 규칙 전체표 — 허용은 정확히 4가지")
	void transitionTable() {
		Set<String> allowed = new TreeSet<>();
		for (PaymentStatus from : PaymentStatus.values()) {
			for (PaymentStatus to : PaymentStatus.values()) {
				if (from.canChangeTo(to)) {
					allowed.add(from + "->" + to);
				}
			}
		}
		assertThat(allowed).containsExactlyInAnyOrder(
				"COMPLETED->REFUND_REQUESTED",
				"REFUND_REQUESTED->REFUNDED", "REFUND_REQUESTED->PARTIALLY_REFUNDED", "REFUND_REQUESTED->COMPLETED");
	}

	private void assertRejected(PaymentStatus from, PaymentStatus to) {
		Payment payment = newPayment();
		ReflectionTestUtils.setField(payment, "paymentStatus", from);

		assertThatThrownBy(() -> payment.changeStatus(to))
				.isInstanceOf(BusinessException.class)
				.hasMessage("결제 상태를 " + from + "에서 " + to + "(으)로 바꿀 수 없습니다.");
		assertThat(payment.getPaymentStatus()).isEqualTo(from);
	}

	private Payment newPayment() {
		return Payment.create(1L, 1L, "사전 결제", new BigDecimal("100000.00"), null, null, null);
	}
}
