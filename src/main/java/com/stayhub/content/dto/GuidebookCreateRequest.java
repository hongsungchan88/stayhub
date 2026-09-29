package com.stayhub.content.dto;

import com.stayhub.content.entity.GuidebookCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 가이드북 등록 요청 (POST /api/guidebooks 의 본문).
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>화면에서 보낸 JSON 을 받는 그릇입니다. 컨트롤러의 {@code @Valid} 가 아래 검증을 실행하고,
 * 틀리면 {@code GlobalExceptionHandler} 가 "title: 제목을 입력해 주세요." 같은 응답을 돌려줍니다.
 *
 * <p>여기 없는 컬럼 — {@code guidebook_id}, {@code property_id}, {@code status}, {@code created_at},
 * {@code updated_at} — 은 요청에서 받지 않습니다. 서버가 정하는 값입니다.
 *
 * <p><b>따라 만들 때 바꿀 곳</b>
 * <ul>
 *   <li>record 이름 — {@code Xxx + CreateRequest}</li>
 *   <li>필드 — 사용자가 입력하는 컬럼만. 정의서의 NOT NULL·최대 길이를 검증 어노테이션으로 옮깁니다</li>
 * </ul>
 * 검증 규칙 옮기는 법: 필수 enum·숫자 → {@code @NotNull}, 필수 문자열 → {@code @NotBlank},
 * 길이 제한 → {@code @Size(max = n)}. 선택 입력은 어노테이션 없이 둡니다.
 */
public record GuidebookCreateRequest( // ★ 바꿀 곳: record 이름

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
