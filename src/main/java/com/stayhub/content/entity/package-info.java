/**
 * 콘텐츠 도메인의 entity 패키지.
 *
 * <p>DB 테이블과 1:1로 대응하는 JPA 엔티티를 두는 곳입니다. BaseEntity 를 상속하세요.
 *
 * <p>들어 있는 파일
 * <ul>
 *   <li>{@code Guidebook} — guidebooks 테이블 한 행</li>
 *   <li>{@code GuidebookCategory} — category 컬럼의 값 목록</li>
 * </ul>
 * 숙소 가이드북(CMS-002)은 새 기능을 만들 때 따라 하는 참고 구현입니다. ({@code docs/how-to-new-feature.md})
 */
package com.stayhub.content.entity;
