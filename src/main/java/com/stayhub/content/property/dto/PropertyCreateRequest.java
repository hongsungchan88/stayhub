package com.stayhub.content.property.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 숙소 등록 요청 (POST /api/properties 의 본문).
 *
 * <p>여기 없는 컬럼 — {@code property_id}, {@code status}, {@code created_at}, {@code updated_at},
 * {@code updated_by} — 은 요청에서 받지 않습니다. 서버가 정하는 값입니다. (status 는 DRAFT 로 시작)
 */
public record PropertyCreateRequest(

		@NotBlank(message = "숙소명을 입력해 주세요.")
		@Size(max = 50, message = "숙소명은 50자 이내로 입력해 주세요.")
		String propertyName,

		@NotBlank(message = "소개 문구를 입력해 주세요.")
		@Size(max = 2000, message = "소개 문구는 2,000자 이내로 입력해 주세요.")
		String description
) {
}
