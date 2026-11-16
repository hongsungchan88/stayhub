package com.stayhub.account.dto;

import com.stayhub.account.entity.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 직원 계정 수정 요청 (PUT /api/users/{id} 의 본문, ACC-005).
 *
 * <p>이름·역할만 바꿀 수 있습니다(총괄 확인). 아이디·이메일·전화번호·비밀번호는 여기서 바꾸지 않습니다.
 * 역할은 FRONT·CLEANING·STAFF 중에서만 고를 수 있고, HOST 는 서비스에서 막습니다.
 */
public record UserUpdateRequest(

		@NotBlank(message = "이름을 입력해 주세요.")
		@Size(max = 50, message = "이름은 50자 이내로 입력해 주세요.")
		String name,

		@NotNull(message = "역할을 선택해 주세요.")
		UserRole role
) {
}
