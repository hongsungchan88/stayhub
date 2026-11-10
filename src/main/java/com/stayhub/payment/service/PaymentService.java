package com.stayhub.payment.service;

import com.stayhub.common.BusinessException;
import com.stayhub.common.PageResponse;
import com.stayhub.payment.dto.PaymentCreateRequest;
import com.stayhub.payment.dto.PaymentRefundRejectRequest;
import com.stayhub.payment.dto.PaymentResponse;
import com.stayhub.payment.dto.PaymentTotalResponse;
import com.stayhub.payment.dto.PaymentUpdateRequest;
import com.stayhub.payment.entity.AdditionalCharge;
import com.stayhub.payment.entity.Payment;
import com.stayhub.payment.entity.PaymentRefundRule;
import com.stayhub.payment.entity.PaymentStatus;
import com.stayhub.payment.repository.AdditionalChargeRepository;
import com.stayhub.payment.repository.PaymentRepository;
import com.stayhub.reservation.entity.Reservation;
import com.stayhub.reservation.repository.ReservationRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제/환불 업무 로직.
 *
 * <p>컨트롤러에게 요청 DTO 를 받아 엔티티를 만들거나 고치고, 결과를 응답 DTO 로 바꿔 돌려줍니다.
 * 엔티티는 이 클래스 밖으로 나가지 않습니다.
 *
 * <p><b>트랜잭션 규칙</b> — 클래스에 {@code @Transactional(readOnly = true)}, 쓰기 메서드(등록·수정)에만 {@code @Transactional}.
 */
@Service
@Transactional(readOnly = true)
public class PaymentService {

	/** 목록 정렬: payment_id 오름차순(등록 순). */
	private static final Sort LIST_SORT = Sort.by(
			Sort.Order.asc("id")
	);

	/** 전액 환불 기준: 체크인 며칠 전까지 요청해야 전액인지. 정의서 "체크인 3일 전까지 전액 환불". */
	static final int FULL_REFUND_DAYS_BEFORE = 3;

	/**
	 * 부분 환불 비율. <b>임시값(50%)</b> — 정의서에 "이후 부분 환불 규정 적용" 만 있고 비율이 없어 총괄 확인 대기 중입니다.
	 * 비율이 정해지면 이 값만 바꾸세요.
	 */
	static final BigDecimal PARTIAL_REFUND_RATE = new BigDecimal("0.50");

	private final PaymentRepository paymentRepository;
	private final AdditionalChargeRepository additionalChargeRepository;
	private final ReservationRepository reservationRepository;

	public PaymentService(
			PaymentRepository paymentRepository,
			AdditionalChargeRepository additionalChargeRepository,
			ReservationRepository reservationRepository
	) {
		this.paymentRepository = paymentRepository;
		this.additionalChargeRepository = additionalChargeRepository;
		this.reservationRepository = reservationRepository;
	}

	/**
	 * 목록. 한 페이지 20건.
	 *
	 * @param page 0부터 시작
	 */
	public PageResponse<PaymentResponse> getList(int page) {
		Pageable pageable = PageRequest.of(page, PageResponse.DEFAULT_PAGE_SIZE, LIST_SORT);
		return PageResponse.from(paymentRepository.findAll(pageable).map(PaymentResponse::from));
	}

	/** 상세. 없는 항목이면 BusinessException. */
	public PaymentResponse getDetail(Long id) {
		return PaymentResponse.from(findOrThrow(id));
	}

	/**
	 * 결제 합계 (PAY-002). 결제 금액에 이 결제로 합산된 추가 요금을 더해 돌려줍니다. 없는 항목이면 BusinessException.
	 *
	 * <p>{@code payment_amount} 는 바꾸지 않고 조회할 때 계산합니다. 수수료·실매출은 결제 금액을 기준으로 저장된 값이라,
	 * 결제 금액을 직접 늘리면 그 값들과 어긋납니다.
	 */
	public PaymentTotalResponse getTotal(Long id) {
		Payment payment = findOrThrow(id);
		BigDecimal mergedChargeAmount = additionalChargeRepository.findByPaymentIdAndIsMergedTrue(id).stream()
				.map(AdditionalCharge::getAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		return new PaymentTotalResponse(
				id,
				payment.getPaymentAmount(),
				mergedChargeAmount,
				payment.getPaymentAmount().add(mergedChargeAmount)
		);
	}

	/** 등록. */
	@Transactional
	public PaymentResponse create(PaymentCreateRequest request) {
		Payment payment = Payment.create(
				request.reservationId(),
				request.channelId(),
				request.paymentMethod(),
				request.paymentAmount(),
				request.commissionAmount(),
				request.netRevenue(),
				request.errorNote()
		);
		return PaymentResponse.from(paymentRepository.save(payment));
	}

	/** 수정. 없는 항목이면 BusinessException. */
	@Transactional
	public PaymentResponse update(Long id, PaymentUpdateRequest request) {
		Payment payment = findOrThrow(id);
		payment.update(
				request.reservationId(),
				request.channelId(),
				request.paymentMethod(),
				request.paymentAmount(),
				request.commissionAmount(),
				request.netRevenue(),
				request.errorNote()
		);
		// save() 를 부르지 않아도 트랜잭션이 끝날 때 바뀐 값이 자동으로 DB 에 반영됩니다. (변경 감지)
		// 다만 수정일시(updatedAt)는 반영되는 순간에 채워지므로, 응답에 새 값을 담으려고 먼저 반영합니다.
		paymentRepository.flush();
		return PaymentResponse.from(payment);
	}

	/**
	 * 환불 요청 (PAY-001). 결제 완료(COMPLETED) 상태에서만 가능하고, 오늘 날짜를 환불 요청일로 저장합니다.
	 * 다른 상태면 {@code Payment.changeStatus} 가 BusinessException 으로 반려합니다.
	 */
	@Transactional
	public PaymentResponse requestRefund(Long id) {
		Payment payment = findOrThrow(id);
		payment.requestRefund(LocalDate.now());
		paymentRepository.flush(); // 수정일시(updatedAt)를 응답에 담으려고 먼저 반영 (update 와 같은 이유)
		return PaymentResponse.from(payment);
	}

	/**
	 * 환불 처리 (PAY-001). 환불 규정을 자동으로 적용해 환불액을 계산하고 상태를 바꿉니다.
	 *
	 * <ul>
	 *   <li>환불 요청일이 체크인 {@value #FULL_REFUND_DAYS_BEFORE}일 전까지면 — 전액. REFUNDED, 규정 FULL, 환불액 = 결제 금액</li>
	 *   <li>그 이후면 — 부분. PARTIALLY_REFUNDED, 규정 PARTIAL, 환불액 = 결제 금액 × {@link #PARTIAL_REFUND_RATE}</li>
	 * </ul>
	 * 체크인 날짜는 결제의 예약(reservations)에서 읽습니다. 합산된 추가 요금은 환불액에 넣지 않습니다.
	 * 환불 요청(REFUND_REQUESTED) 상태가 아니면 BusinessException.
	 */
	@Transactional
	public PaymentResponse refund(Long id) {
		Payment payment = findOrThrow(id);
		if (payment.getPaymentStatus() != PaymentStatus.REFUND_REQUESTED) {
			// 아래 계산에 요청일이 필요해서, changeStatus 보다 먼저 같은 규칙으로 막습니다.
			throw new BusinessException("환불 요청 상태의 결제만 환불할 수 있습니다.");
		}
		LocalDate checkInDate = reservationRepository.findById(payment.getReservationId())
				.map(Reservation::getCheckInDate)
				.orElseThrow(() -> new BusinessException("결제의 예약을 찾을 수 없습니다."));

		if (isFullRefund(payment.getRefundRequestDate(), checkInDate)) {
			payment.refund(PaymentRefundRule.FULL, payment.getPaymentAmount());
		} else {
			payment.refund(PaymentRefundRule.PARTIAL, partialAmount(payment.getPaymentAmount()));
		}
		paymentRepository.flush();
		return PaymentResponse.from(payment);
	}

	/** 환불 거절 (PAY-001). 환불 요청 상태를 결제 완료로 되돌리고, 사유를 error_note 에 남깁니다. */
	@Transactional
	public PaymentResponse rejectRefund(Long id, PaymentRefundRejectRequest request) {
		Payment payment = findOrThrow(id);
		payment.rejectRefund(request.reason());
		paymentRepository.flush();
		return PaymentResponse.from(payment);
	}

	/** 체크인 N일 전까지 요청했으면 전액. 예) 체크인 11/20 이면 11/17 까지 요청한 건 전액, 11/18 부터 부분. */
	static boolean isFullRefund(LocalDate requestDate, LocalDate checkInDate) {
		return !requestDate.isAfter(checkInDate.minusDays(FULL_REFUND_DAYS_BEFORE));
	}

	/** 부분 환불액. 소수 셋째 자리에서 반올림(DECIMAL(10,2)). */
	static BigDecimal partialAmount(BigDecimal paymentAmount) {
		return paymentAmount.multiply(PARTIAL_REFUND_RATE).setScale(2, RoundingMode.HALF_UP);
	}

	/** 항목 하나를 찾습니다. 없으면 예외. */
	private Payment findOrThrow(Long id) {
		return paymentRepository.findById(id)
				.orElseThrow(() -> new BusinessException("결제 내역을 찾을 수 없습니다."));
	}
}
