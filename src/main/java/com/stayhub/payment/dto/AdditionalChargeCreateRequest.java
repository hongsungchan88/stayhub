package com.stayhub.payment.dto;

import com.stayhub.payment.entity.AdditionalChargeType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 추가 요금 등록 요청 (POST /api/additional-charges 의 본문).
 *
 * <p>여기 없는 컬럼 — {@code charge_id}, {@code charged_at}, {@code created_at}, {@code updated_at} — 은
 * 요청에서 받지 않습니다. 서버가 정하는 값입니다.
 */
public record AdditionalChargeCreateRequest(

		@NotNull(message = "예약을 선택해 주세요.")
		Long reservationId,

		@NotNull(message = "요금 유형을 선택해 주세요.")
		AdditionalChargeType chargeType,

		// 선택 입력.
		@Size(max = 255, message = "상세 내역은 255자 이내로 입력해 주세요.")
		String description,

		@NotNull(message = "추가 요금 금액을 입력해 주세요.")
		BigDecimal amount,

		// 선택 입력. 비우면 false.
		Boolean isMerged,

		// 선택 입력. 기존 결제에 합산할 때만.
		Long paymentId,

		// 선택 입력. 비우면 false.
		Boolean isSettled
) {
}
