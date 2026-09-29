package com.stayhub.content.dto;

import com.stayhub.content.entity.GuidebookCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 가이드북 수정 요청 (PUT /api/guidebooks/{id} 의 본문).
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>등록 요청과 같은 모양입니다. PUT 은 "통째로 바꾸기" 라서 모든 값을 다시 보냅니다.
 * sortOrder 를 비워 보내면 0 으로 돌아갑니다.
 *
 * <p>등록과 모양이 같아도 파일을 따로 두는 이유: 나중에 "수정할 때는 카테고리를 못 바꾼다" 같은
 * 규칙이 생기면 이 파일만 고치면 됩니다. 하나로 합쳐두면 등록까지 같이 바뀝니다.
 *
 * <p><b>따라 만들 때 바꿀 곳</b>
 * <br>{@code GuidebookCreateRequest} 와 똑같이 바꾸고, 이름만 {@code Xxx + UpdateRequest} 로.
 */
public record GuidebookUpdateRequest( // ★ 바꿀 곳: record 이름

		@NotNull(message = "카테고리를 선택해 주세요.") // ★ 바꿀 곳: 검증 메시지
		GuidebookCategory category, // ★ 바꿀 곳: 타입·필드명

		@NotBlank(message = "제목을 입력해 주세요.") // ★ 바꿀 곳: 검증 메시지
		@Size(max = 50, message = "제목은 50자 이내로 입력해 주세요.") // ★ 바꿀 곳: 정의서의 최대 길이
		String title, // ★ 바꿀 곳: 필드명

		@NotBlank(message = "내용을 입력해 주세요.") // ★ 바꿀 곳: 검증 메시지
		@Size(max = 1000, message = "내용은 1,000자 이내로 입력해 주세요.") // ★ 바꿀 곳: 정의서의 최대 길이
		String content, // ★ 바꿀 곳: 필드명

		// 선택 입력. 비우면 0 (등록 순).
		Integer sortOrder // ★ 바꿀 곳: 필드명 (선택 입력이라 검증 없음)
) {
}
