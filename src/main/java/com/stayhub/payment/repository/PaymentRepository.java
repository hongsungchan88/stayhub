package com.stayhub.payment.repository;

import com.stayhub.payment.entity.Payment;
import com.stayhub.payment.entity.PaymentStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 결제/환불 DB 접근.
 *
 * <p>삭제 기능이 없어서 {@code JpaRepository} 기본 메서드 {@code findAll(pageable)}·{@code findById}·{@code save} 를 주로 씁니다.
 */
public interface PaymentRepository extends JpaRepository<Payment, Long> {

	/** 한 예약의 결제 중 특정 상태인 것. 예약 취소 환불(RSV-002)에서 결제 완료 건을 고를 때 씁니다. */
	List<Payment> findByReservationIdAndPaymentStatus(Long reservationId, PaymentStatus paymentStatus);
}
