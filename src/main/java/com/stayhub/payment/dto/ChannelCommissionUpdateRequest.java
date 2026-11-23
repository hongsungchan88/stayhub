package com.stayhub.payment.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 채널 수수료율 수정 요청 (PUT /api/channel-commissions/{id} 의 본문). PAY-003, SCR-PAY-03
 *
 * <p>두 값 모두 % 단위이고 0~100, 소수 둘째 자리까지입니다(테이블 정의서 {@code DECIMAL(5,2)}).
 * 등록 채널은 {@code commissionRate}, 미등록 채널은 {@code defaultCommissionRate} 로 수수료를 계산합니다.
 */
public record ChannelCommissionUpdateRequest(

		@NotNull(message = "수수료율을 입력해 주세요.")
		@DecimalMin(value = "0.00", message = "수수료율은 0 이상으로 입력해 주세요.")
		@DecimalMax(value = "100.00", message = "수수료율은 100 이하로 입력해 주세요.")
		@Digits(integer = 3, fraction = 2, message = "수수료율은 소수 둘째 자리까지 입력해 주세요.")
		BigDecimal commissionRate,

		@NotNull(message = "기본 수수료율을 입력해 주세요.")
		@DecimalMin(value = "0.00", message = "기본 수수료율은 0 이상으로 입력해 주세요.")
		@DecimalMax(value = "100.00", message = "기본 수수료율은 100 이하로 입력해 주세요.")
		@Digits(integer = 3, fraction = 2, message = "기본 수수료율은 소수 둘째 자리까지 입력해 주세요.")
		BigDecimal defaultCommissionRate
) {
}
