package com.stayhub.account.entity;

/**
 * 이메일 인증 유형.
 *
 * <p>테이블 정의서의 {@code email_verifications.type VARCHAR(20) — JOIN(가입), PASSWORD_RESET(재설정)} 을 옮긴 것입니다.
 * DB 에는 {@code "JOIN"} 처럼 이름 그대로 저장됩니다. 기본값은 {@code JOIN} 입니다.
 */
public enum VerificationType {

	/** 회원가입 이메일 인증 (24시간 유효, ACC-001) */
	JOIN,

	/** 비밀번호 재설정 (30분 유효, ACC-003 에서 사용) */
	PASSWORD_RESET
}
