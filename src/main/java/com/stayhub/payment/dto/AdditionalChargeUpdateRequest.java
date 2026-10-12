package com.stayhub.payment.dto;

import com.stayhub.payment.entity.AdditionalChargeType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 추가 요금 수정 요청 (PUT /api/additional-charges/{id} 의 본문).
 *
 * <p>등록 요청과 같은 모양입니다. PUT 은 "통째로 바꾸기" 라서 모든 값을 다시 보냅니다.
 * isMerged·isSettled 를 비워 보내면 false 로 돌아갑니다.
 */
public record AdditionalChargeUpdateRequest(

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
