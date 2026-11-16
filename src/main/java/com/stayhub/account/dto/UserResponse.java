package com.stayhub.account.dto;

import com.stayhub.account.entity.User;
import com.stayhub.account.entity.UserRole;
import java.time.LocalDateTime;

/**
 * 직원 계정 응답 (목록·상세·등록·수정이 모두 이 모양으로 나갑니다, ACC-005).
 *
 * <p>비밀번호(암호화 값), 소프트 삭제용 status, 로그인 실패 횟수·잠금 시각은 내부용이라 넣지 않습니다.
 * 변환({@link #from})은 서비스 안에서 호출합니다. 컨트롤러는 엔티티를 만지지 않습니다.
 */
public record UserResponse(
		Long id,
		String loginId,
		String name,
		String phoneNumber,
		String email,
		UserRole role,
		boolean verified,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {

	/** 엔티티를 응답 모양으로 바꿉니다. */
	public static UserResponse from(User user) {
		return new UserResponse(
				user.getId(),
				user.getLoginId(),
				user.getName(),
				user.getPhoneNumber(),
				user.getEmail(),
				user.getRole(),
				user.isVerified(),
				user.getCreatedAt(),
				user.getUpdatedAt()
		);
	}
}
