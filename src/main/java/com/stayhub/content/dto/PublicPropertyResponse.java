package com.stayhub.content.dto;

import java.util.List;

/**
 * 게스트용 숙소 소개 응답 (GET /api/public/property).
 *
 * <p>게스트 화면에 필요한 값만 담습니다. 게시 상태·숙소 ID·작성자·일시 같은 내부 값은 넣지 않습니다.
 *
 * @param propertyName 숙소명
 * @param description  소개 문구. 리치텍스트 HTML 원문 그대로 — 화면에서 반드시 esc() 후 표시
 * @param facilities   숙소에 체크된 시설명 (시설 sort_order 순)
 * @param images       사진 file_path (sort_order 순)
 */
public record PublicPropertyResponse(
		String propertyName,
		String description,
		List<String> facilities,
		List<String> images
) {
}
