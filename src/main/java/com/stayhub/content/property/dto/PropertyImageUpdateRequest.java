package com.stayhub.content.property.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 숙소 이미지 수정 요청 (PUT /api/property_images/{id} 의 본문).
 *
 * <p>등록 요청과 같은 모양입니다. PUT 은 "통째로 바꾸기" 라서 모든 값을 다시 보냅니다.
 * isMain 을 비워 보내면 false, sortOrder 를 비워 보내면 0 으로 돌아갑니다.
 */
public record PropertyImageUpdateRequest(

		@NotBlank(message = "파일 경로를 입력해 주세요.")
		@Size(max = 255, message = "파일 경로는 255자 이내로 입력해 주세요.")
		String filePath,

		// CMS-001: 10MB(10 * 1024 * 1024 byte) 초과 시 업로드 거부
		@NotNull(message = "파일 용량을 입력해 주세요.")
		@Max(value = 10485760, message = "10MB를 초과하는 파일은 업로드할 수 없습니다.")
		Integer fileSize,

		// CMS-001: jpg, png만 허용, 그 외 업로드 거부
		@NotBlank(message = "파일 확장자를 입력해 주세요.")
		@Pattern(regexp = "jpg|png", message = "jpg, png 파일만 업로드할 수 있습니다.")
		String fileExt,

		// 선택 입력. 비우면 false.
		Boolean isMain,

		// 선택 입력. 비우면 0 (등록 순).
		Integer sortOrder
) {
}
