package com.stayhub.account.dto;

import com.stayhub.account.entity.User;
import com.stayhub.account.entity.UserRole;
import java.time.LocalDateTime;

/**
 * 회원가입 결과 응답.
 *
 * <p>비밀번호는 암호화된 값이라도 절대 내보내지 않습니다.
 * status, failed_attempts, lockout_until 도 내부용이라 뺍니다.
 */
public record SignupResponse(
		Long id,
		String loginId,
		String name,
		String phoneNumber,
		String email,
		UserRole role,
		boolean verified,
		LocalDateTime createdAt
) {

	/** 엔티티를 응답 모양으로 바꿉니다. */
	public static SignupResponse from(User user) {
		return new SignupResponse(
				user.getId(),
				user.getLoginId(),
				user.getName(),
				user.getPhoneNumber(),
				user.getEmail(),
				user.getRole(),
				user.isVerified(),
				user.getCreatedAt()
		);
	}
}
