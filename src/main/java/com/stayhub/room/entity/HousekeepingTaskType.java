package com.stayhub.room.entity;

/**
 * 청소/점검 작업 유형.
 *
 * <p>테이블 정의서의 {@code task_type VARCHAR(20) — CLEANING, INSPECTION} 을 옮긴 것입니다.
 * DB 에는 {@code "CLEANING"} 처럼 이름 그대로 저장됩니다. (엔티티의 {@code @Enumerated(EnumType.STRING)} 덕분)
 */
public enum HousekeepingTaskType {

	/** 청소 */
	CLEANING,

	/** 점검 */
	INSPECTION
}
