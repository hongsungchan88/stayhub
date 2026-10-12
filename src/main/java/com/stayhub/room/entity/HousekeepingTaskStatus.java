package com.stayhub.room.entity;

/**
 * 청소/점검 작업 상태 (ROM-003).
 *
 * <p>테이블 정의서의 {@code status VARCHAR(20) — SCHEDULED, IN_PROGRESS, COMPLETED} 를 옮긴 것입니다.
 * 업무 상태라서 {@code common/EntityStatus} 를 쓰지 않고 객실 도메인 enum 으로 따로 둡니다.
 */
public enum HousekeepingTaskStatus {

	/** 배정됨 */
	SCHEDULED,

	/** 진행 중 */
	IN_PROGRESS,

	/** 완료 */
	COMPLETED
}
