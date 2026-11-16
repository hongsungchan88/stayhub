package com.stayhub.content.dto;

import com.stayhub.content.entity.GuidebookCategory;

/**
 * 게스트용 가이드북 응답 (GET /api/public/guidebooks).
 *
 * <p>게스트 화면에 필요한 값만 담습니다. id·status·propertyId·일시 같은 내부 값은 넣지 않습니다.
 */
public record PublicGuidebookResponse(
		GuidebookCategory category,
		String title,
		String content,
		int sortOrder
) {

	/** 관리자용 응답에서 게스트에게 보일 값만 옮겨 담습니다. */
	public static PublicGuidebookResponse from(GuidebookResponse guidebook) {
		return new PublicGuidebookResponse(
				guidebook.category(),
				guidebook.title(),
				guidebook.content(),
				guidebook.sortOrder()
		);
	}
}
