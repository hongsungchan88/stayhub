package com.stayhub.account.repository;

import com.stayhub.account.entity.EmailVerification;
import com.stayhub.account.entity.VerificationType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 이메일 인증 기록 DB 접근.
 *
 * <p>유형 ③(status 없음)이라 status 조건이 없습니다.
 */
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

	/**
	 * 이 이메일로 보낸 가장 최근 인증 기록.
	 * 재전송하면 새 행이 생기므로, 확인할 때는 항상 마지막 것만 봅니다(이전 코드는 무효).
	 */
	Optional<EmailVerification> findTopByEmailAndTypeOrderByIdDesc(String email, VerificationType type);

	/** 이 이메일에 인증 완료된 기록이 있는지. (가입 저장 전 확인) */
	boolean existsByEmailAndTypeAndUsed(String email, VerificationType type, boolean used);

	/** 재설정 링크의 토큰으로 기록 찾기. (ACC-003 — 링크에는 이메일 없이 토큰만 들어 있습니다) */
	Optional<EmailVerification> findByCodeAndType(String code, VerificationType type);
}
