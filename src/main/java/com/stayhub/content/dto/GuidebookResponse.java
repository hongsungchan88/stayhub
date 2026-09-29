package com.stayhub.content.dto;

import com.stayhub.content.entity.Guidebook;
import com.stayhub.content.entity.GuidebookCategory;
import java.time.LocalDateTime;

/**
 * 가이드북 응답 (목록·상세·등록·수정이 모두 이 모양으로 나갑니다).
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>엔티티를 화면에 보낼 모양으로 옮겨 담습니다. 엔티티를 그대로 내보내지 않는 이유:
 * <ul>
 *   <li>내부용 값(소프트 삭제용 status, propertyId)까지 밖으로 새어 나갑니다</li>
 *   <li>테이블에 컬럼을 추가하는 순간 API 응답 모양이 같이 바뀌어 화면이 깨질 수 있습니다</li>
 * </ul>
 * 변환({@link #from})은 서비스 안에서 호출합니다. 컨트롤러는 엔티티를 만지지 않습니다.
 *
 * <p><b>따라 만들 때 바꿀 곳</b>
 * <ul>
 *   <li>record 이름 — {@code Xxx + Response}</li>
 *   <li>필드 — 소프트 삭제용 status(EntityStatus)와 propertyId는 뺀다.
 *       예약 상태처럼 업무 상태, roomId처럼 화면에 필요한 참조 ID는 넣는다.</li>
 *   <li>{@code from} 안의 getter 호출 — 위 필드와 순서를 맞춰서</li>
 * </ul>
 */
public record GuidebookResponse( // ★ 바꿀 곳: record 이름
		Long id,
		GuidebookCategory category, // ★ 바꿀 곳: 필드
		String title, // ★ 바꿀 곳: 필드
		String content, // ★ 바꿀 곳: 필드
		int sortOrder, // ★ 바꿀 곳: 필드
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {

	/** 엔티티를 응답 모양으로 바꿉니다. */
	public static GuidebookResponse from(Guidebook guidebook) { // ★ 바꿀 곳: 응답·엔티티 타입
		return new GuidebookResponse( // ★ 바꿀 곳: record 이름
				guidebook.getId(),
				guidebook.getCategory(), // ★ 바꿀 곳: getter
				guidebook.getTitle(), // ★ 바꿀 곳: getter
				guidebook.getContent(), // ★ 바꿀 곳: getter
				guidebook.getSortOrder(), // ★ 바꿀 곳: getter
				guidebook.getCreatedAt(),
				guidebook.getUpdatedAt()
		);
	}
}
