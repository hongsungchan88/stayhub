package com.stayhub.account.entity;

/**
 * 계정 역할(권한).
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>테이블 정의서의 {@code users.role VARCHAR(20) — HOST, FRONT, CLEANING, STAFF} 를 옮긴 것입니다.
 * DB 에는 {@code "HOST"} 처럼 이름 그대로 저장됩니다. 기본값은 {@code STAFF} 입니다.
 * 역할별로 어떤 화면·API 에 들어갈 수 있는지는 여기서 정하지 않습니다(권한 매핑은 ACC-005 작업에서).
 *
 * <p><b>따라 만들 때 바꿀 곳</b>
 * <br>없습니다. 역할이 늘어나면 정의서를 먼저 고치고 그 값을 대문자 그대로 추가합니다.
 */
public enum UserRole {

	/** 호스트(관리자). 직원 계정 생성·승인 처리 */
	HOST,

	/** 프런트 */
	FRONT,

	/** 청소 */
	CLEANING,

	/** 일반 스태프. 기본값 */
	STAFF
}
