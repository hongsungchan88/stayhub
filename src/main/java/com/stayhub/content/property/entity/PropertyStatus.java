package com.stayhub.content.property.entity;

/**
 * 숙소 게시 상태.
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>테이블 정의서의 {@code properties.status VARCHAR(20)} 을 옮긴 것입니다.
 * 소프트 삭제용 {@code EntityStatus} 가 아니라 업무 상태(유형 ②)라서 도메인 enum 을 따로 둡니다.
 *
 * <p>1차 개발은 DRAFT / PUBLISHED 만 사용합니다.
 * PENDING 은 v2 이관 기능 CMS-004 에서 추가합니다.
 */
public enum PropertyStatus {

	/** 임시 저장 (등록 시 첫 상태) */
	DRAFT,

	/** 게시 */
	PUBLISHED
}
