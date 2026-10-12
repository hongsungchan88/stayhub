package com.stayhub.content.property.dto;

import com.stayhub.content.property.entity.PropertyStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 숙소 수정 요청 (PUT /api/properties/{id} 의 본문).
 *
 * <p>PUT 은 "통째로 바꾸기" 라서 모든 값을 다시 보냅니다.
 * 등록과 달리 게시 상태(DRAFT·PUBLISHED)를 함께 받아, 게시·게시 취소를 수정으로 처리합니다.
 */
public record PropertyUpdateRequest(

		@NotBlank(message = "숙소명을 입력해 주세요.")
		@Size(max = 50, message = "숙소명은 50자 이내로 입력해 주세요.")
		String propertyName,

		@NotBlank(message = "소개 문구를 입력해 주세요.")
		@Size(max = 2000, message = "소개 문구는 2,000자 이내로 입력해 주세요.")
		String description,

		@NotNull(message = "게시 상태를 선택해 주세요.")
		PropertyStatus status
) {
}
