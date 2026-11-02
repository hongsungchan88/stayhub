package com.stayhub.payment.repository;

import com.stayhub.payment.entity.AdditionalCharge;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 추가 요금 DB 접근.
 *
 * <p>삭제 기능이 없고 목록 필터도 없어서, 목록·상세는 {@code JpaRepository} 기본 메서드
 * {@code findAll(pageable)}·{@code findById} 를 씁니다.
 * 따로 선언한 메서드는 결제 합계를 계산하는 용도뿐입니다.
 */
public interface AdditionalChargeRepository extends JpaRepository<AdditionalCharge, Long> {

	/** 이 결제로 합산된 추가 요금 목록. (결제 합계를 계산할 때) */
	List<AdditionalCharge> findByPaymentIdAndIsMergedTrue(Long paymentId);
}
