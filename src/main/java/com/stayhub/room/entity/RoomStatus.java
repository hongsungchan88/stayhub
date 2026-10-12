package com.stayhub.room.entity;

/**
 * 현재 객실 상태 (ROM-001).
 *
 * <p>테이블 정의서의 {@code status VARCHAR(20) — VACANT, CLEANING_NEEDED, CLEANING, OCCUPIED} 를 옮긴 것입니다.
 * DB 에는 {@code "VACANT"} 처럼 이름 그대로 저장됩니다. (엔티티의 {@code @Enumerated(EnumType.STRING)} 덕분)
 *
 * <p>업무 상태라서 {@code common/EntityStatus} 를 쓰지 않고 객실 도메인 enum 으로 따로 둡니다.
 */
public enum RoomStatus {

	/** 빈방 */
	VACANT,

	/** 청소필요 */
	CLEANING_NEEDED,

	/** 청소중 */
	CLEANING,

	/** 투숙중 */
	OCCUPIED
}
