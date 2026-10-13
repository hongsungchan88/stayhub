package com.stayhub.payment.entity;

/**
 * 추가 요금 유형 (PAY-002).
 *
 * <p>테이블 정의서의 {@code charge_type VARCHAR(20) — MINIBAR, EARLY_CHECKIN, LATE_CHECKOUT, SERVICE} 를 옮긴 것입니다.
 */
public enum AdditionalChargeType {

	/** 미니바 */
	MINIBAR,

	/** 얼리 체크인 */
	EARLY_CHECKIN,

	/** 레이트 체크아웃 */
	LATE_CHECKOUT,

	/** 부대 서비스 */
	SERVICE
}
