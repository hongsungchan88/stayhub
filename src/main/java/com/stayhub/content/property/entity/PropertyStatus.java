package com.stayhub.content.property.entity;

/**
 * 숙소 게시 상태.
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>테이블 정의서의 {@code properties.status VARCHAR(20) — 기본값 "DRAFT"} 를 옮긴 것입니다.
 * DB 에는 {@code "DRAFT"} 처럼 이름 그대로 저장됩니다. (엔티티의 {@code @Enumerated(EnumType.STRING)} 덕분)
 *
 * <p>유형 ② 업무 상태입니다. 소프트 삭제용 ACTIVE / DELETED 가 아니므로 {@code common/EntityStatus} 를 쓰지 않습니다.
 * CMS-004(v2 이관): 1차 개발은 DRAFT / PUBLISHED 만 사용합니다. PENDING 은 v2 에서 추가합니다.
 */
public enum PropertyStatus {

	/** 작성 중 (미게시) */
	DRAFT,

	/** 게시됨 */
	PUBLISHED
}
