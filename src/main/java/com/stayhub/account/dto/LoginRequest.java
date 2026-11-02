package com.stayhub.account.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 로그인 요청 (POST /api/auth/login 의 본문).
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>아이디·비밀번호가 비어 있는지만 확인합니다. 형식(4~20자 등)은 검사하지 않습니다.
 * 형식이 틀린 아이디는 어차피 DB 에 없으므로 "아이디 또는 비밀번호가 올바르지 않습니다." 로 끝납니다.
 */
public record LoginRequest(

		@NotBlank(message = "아이디를 입력해 주세요.")
		String loginId,

		@NotBlank(message = "비밀번호를 입력해 주세요.")
		String password
) {
}
