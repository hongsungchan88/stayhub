package com.stayhub.room.entity;

/**
 * 가격 규칙 유형. [v2 이관 · 스키마만 유지]
 *
 * <p>테이블 정의서의 {@code rule_type VARCHAR(20) — WEEKDAY, SEASON, OCCUPANCY, IMMINENT} 를 옮긴 것입니다.
 */
public enum PriceRuleType {

	/** 요일 */
	WEEKDAY,

	/** 성수기 */
	SEASON,

	/** 잔여 객실 */
	OCCUPANCY,

	/** 체크인 임박 */
	IMMINENT
}
