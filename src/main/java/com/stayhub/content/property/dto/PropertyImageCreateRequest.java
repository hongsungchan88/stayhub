package com.stayhub.content.property.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 숙소 이미지 등록 요청 (POST /api/property-images 의 본문).
 *
 * <p>여기 없는 컬럼 — {@code image_id}, {@code property_id}, {@code created_at} — 은 요청에서 받지 않습니다.
 * 서버가 정하는 값입니다.
 *
 * <p>CMS-001 업로드 제한: 10MB(10,485,760 byte) 초과, jpg/png 외 확장자는 거부합니다.
 */
public record PropertyImageCreateRequest(

		@NotBlank(message = "파일 경로를 입력해 주세요.")
		@Size(max = 255, message = "파일 경로는 255자 이내로 입력해 주세요.")
		String filePath,

		@NotNull(message = "파일 용량을 입력해 주세요.")
		@Max(value = 10_485_760, message = "10MB 이하의 파일만 업로드할 수 있습니다.")
		Integer fileSize,

		@NotBlank(message = "파일 확장자를 입력해 주세요.")
		@Pattern(regexp = "jpg|png", message = "jpg, png 파일만 업로드할 수 있습니다.")
		String fileExt,

		// 선택 입력. 비우면 false.
		Boolean isMain,

		// 선택 입력. 비우면 0 (등록 순).
		Integer sortOrder
) {
}
