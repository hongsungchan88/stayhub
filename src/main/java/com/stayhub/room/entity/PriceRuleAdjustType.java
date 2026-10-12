package com.stayhub.room.entity;

/**
 * 가격 조정 방식. [v2 이관 · 스키마만 유지]
 *
 * <p>테이블 정의서의 {@code adjust_type VARCHAR(10) — PERCENT, FIXED} 를 옮긴 것입니다.
 */
public enum PriceRuleAdjustType {

	/** 비율 */
	PERCENT,

	/** 정액 */
	FIXED
}
