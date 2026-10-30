package com.stayhub.account.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 회원가입 인증코드 확인 요청 (POST /api/signup/email-code/verify 의 본문).
 */
public record EmailCodeVerifyRequest(

		@NotBlank(message = "이메일을 입력해 주세요.")
		@Email(message = "이메일 형식이 올바르지 않습니다.")
		@Size(max = 100, message = "이메일은 100자 이내로 입력해 주세요.")
		String email,

		@NotBlank(message = "인증코드를 입력해 주세요.")
		String code
) {
}
