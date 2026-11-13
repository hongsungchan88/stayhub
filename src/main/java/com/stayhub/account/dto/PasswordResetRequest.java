package com.stayhub.account.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 비밀번호 재설정 링크 요청 (POST /api/password-reset/request 의 본문).
 *
 * <p>기능 정의서 ACC-003 입력 항목 "아이디 또는 이메일" 을 한 칸으로 받습니다.
 * {@code @} 가 들어 있으면 이메일, 없으면 아이디로 찾습니다. (아이디는 영문+숫자만이라 {@code @} 가 들어갈 수 없습니다)
 */
public record PasswordResetRequest(

		@NotBlank(message = "아이디 또는 이메일을 입력해 주세요.")
		String loginIdOrEmail
) {
}
