package com.stayhub.payment.repository;

import com.stayhub.payment.entity.AdditionalCharge;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 추가 요금 DB 접근.
 *
 * <p>삭제 기능이 없고 목록 필터도 없어서, 목록·상세는 {@code JpaRepository} 기본 메서드
 * {@code findAll(pageable)}·{@code findById} 를 씁니다.
 * 따로 선언한 메서드는 결제 합계 계산과 체크아웃 전 미정산 확인 용도입니다.
 */
public interface AdditionalChargeRepository extends JpaRepository<AdditionalCharge, Long> {

	/** 이 결제로 합산된 추가 요금 목록. (결제 합계를 계산할 때) */
	List<AdditionalCharge> findByPaymentIdAndIsMergedTrue(Long paymentId);

	/** 이 예약에 정산되지 않은(is_settled = FALSE) 추가 요금이 하나라도 있는지. (체크아웃 전 확인) */
	boolean existsByReservationIdAndIsSettledFalse(Long reservationId);
}
