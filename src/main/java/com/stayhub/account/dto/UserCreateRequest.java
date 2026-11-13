package com.stayhub.account.dto;

import com.stayhub.account.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 직원 계정 등록 요청 (POST /api/users 의 본문, ACC-005).
 *
 * <p>호스트가 이메일·전화번호·초기 비밀번호까지 입력합니다(총괄 확인).
 * 형식 검증은 회원가입({@code SignupRequest})과 똑같이 맞춥니다. 가입 규칙이 바뀌면 여기도 같이 바꿉니다.
 *
 * <p>여기 없는 컬럼 — {@code user_id}, {@code is_verified}(항상 FALSE), {@code failed_attempts},
 * {@code lockout_until}, {@code status}, {@code created_at}, {@code updated_at} — 은 서버가 정하는 값입니다.
 * 역할은 FRONT·CLEANING·STAFF 중에서만 고를 수 있고, HOST 는 서비스에서 막습니다.
 */
public record UserCreateRequest(

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
		String email,

		@NotNull(message = "역할을 선택해 주세요.")
		UserRole role
) {
}
