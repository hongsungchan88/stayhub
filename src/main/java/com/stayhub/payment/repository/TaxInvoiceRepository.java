package com.stayhub.payment.repository;

import com.stayhub.payment.entity.TaxInvoice;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 세금 계산서/현금 영수증 DB 접근.
 *
 * <p>삭제 기능이 없고 목록 필터도 없어서, {@code JpaRepository} 기본 메서드
 * {@code findAll(pageable)}·{@code findById}·{@code save} 로 충분합니다. 따로 선언한 메서드가 없습니다.
 */
public interface TaxInvoiceRepository extends JpaRepository<TaxInvoice, Long> {
}
