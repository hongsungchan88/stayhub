package com.stayhub.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 새 비밀번호 저장 요청 (POST /api/password-reset/confirm 의 본문).
 *
 * <p>{@code token} 은 재설정 링크에 들어 있는 값(서버 로그에 출력된 UUID)입니다.
 * 새 비밀번호 규칙은 회원가입(SignupRequest.password)과 같습니다 — 정의서 users.password "8자 이상, 특수문자 포함".
 * 다르게 두면 가입은 되는데 재설정이 안 되는 비밀번호가 생기므로, 가입 규칙이 바뀌면 여기도 같이 바꿉니다.
 */
public record PasswordResetConfirmRequest(

		@NotBlank(message = "재설정 링크가 올바르지 않습니다.")
		String token,

		@NotBlank(message = "새 비밀번호를 입력해 주세요.")
		@Size(min = 8, message = "비밀번호는 8자 이상 입력해 주세요.")
		@Pattern(regexp = ".*[^A-Za-z0-9].*", message = "비밀번호에 특수문자를 1개 이상 넣어 주세요.")
		String newPassword
) {
}