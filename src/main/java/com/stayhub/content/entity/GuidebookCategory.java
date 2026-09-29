package com.stayhub.content.entity;

/**
 * 가이드북 카테고리.
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>테이블 정의서의 {@code category VARCHAR(20) — PARKING, WIFI, CHECKIN, ETC} 를 옮긴 것입니다.
 * DB 에는 {@code "WIFI"} 처럼 이름 그대로 저장됩니다. (엔티티의 {@code @Enumerated(EnumType.STRING)} 덕분)
 *
 * <p><b>따라 만들 때 바꿀 곳</b>
 * <ul>
 *   <li>파일명·enum 이름 — {@code 테이블단수형 + 컬럼명}. 예) {@code RoomType}, {@code PaymentMethod}</li>
 *   <li>값 목록 — 테이블 정의서에 적힌 값을 대문자 그대로, 한 글자도 다르지 않게</li>
 * </ul>
 * 소프트 삭제용 ACTIVE / DELETED 는 여기 넣지 않습니다. 그건 {@code common/EntityStatus} 가 맡습니다.
 */
public enum GuidebookCategory { // ★ 바꿀 곳: enum 이름

	/** 주차 */
	PARKING, // ★ 바꿀 곳: 정의서의 값

	/** 와이파이 */
	WIFI, // ★ 바꿀 곳: 정의서의 값

	/** 체크인 */
	CHECKIN, // ★ 바꿀 곳: 정의서의 값

	/** 기타 */
	ETC // ★ 바꿀 곳: 정의서의 값
}
