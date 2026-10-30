package com.stayhub.account.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 회원가입 요청 (POST /api/signup 의 본문).
 *
 * <p>형식 검증은 여기서 합니다. User 엔티티에는 암호화된 비밀번호만 들어가서 거기서는 검사할 수 없습니다(총괄 User 주석).
 *
 * <p>여기 없는 컬럼 — role(STAFF 고정), is_verified, failed_attempts, lockout_until, status — 은 서버가 정합니다.
 * 기능 정의서의 "나이" 는 users 테이블에 컬럼이 없어 이번에는 받지 않습니다(리뷰에서 확인 예정).
 */
public record SignupRequest(

		@NotBlank(message = "아이디를 입력해 주세요.")
		@Pattern(regexp = "^[A-Za-z0-9]{4,20}$", message = "아이디는 영문·숫자 4~20자로 입력해 주세요.")
		String loginId,

		@NotBlank(message = "비밀번호를 입력해 주세요.")
		@Size(min = 8, message = "비밀번호는 8자 이상 입력해 주세요.")
		@Pattern(regexp = ".*[^A-Za-z0-9].*", message = "비밀번호에 특수문자를 1개 이상 넣어 주세요.")
		String password,

		@NotBlank(message = "이름을 입력해 주세요.")
		@Size(max = 50, message = "이름은 50자 이내로 입력해 주세요.")
		String name,

		@NotBlank(message = "전화번호를 입력해 주세요.")
		@Size(max = 20, message = "전화번호는 20자 이내로 입력해 주세요.")
		String phoneNumber,

		@NotBlank(message = "이메일을 입력해 주세요.")
		@Email(message = "이메일 형식이 올바르지 않습니다.")
		@Size(max = 100, message = "이메일은 100자 이내로 입력해 주세요.")
		String email
) {
}
