package com.stayhub.account.service;

import com.stayhub.account.dto.PasswordResetConfirmRequest;
import com.stayhub.account.dto.PasswordResetRequest;
import com.stayhub.account.entity.EmailVerification;
import com.stayhub.account.entity.User;
import com.stayhub.account.entity.VerificationType;
import com.stayhub.account.repository.EmailVerificationRepository;
import com.stayhub.account.repository.UserRepository;
import com.stayhub.common.BusinessException;
import com.stayhub.common.EntityStatus;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 비밀번호 찾기·재설정 업무 로직 (ACC-003).
 *
 * <p><b>흐름</b>
 * <ol>
 *   <li>재설정 링크 요청 — {@link #requestReset}
 *       (아이디 또는 이메일로 계정 확인 → 그 계정 이메일로 UUID 토큰 저장(30분) → 서버 로그에 출력)</li>
 *   <li>새 비밀번호 저장 — {@link #resetPassword}
 *       (토큰 확인 → 비밀번호 변경 + 로그인 실패 횟수·잠금 해제 → 토큰 사용 완료)</li>
 * </ol>
 * 기존 세션 만료({@code LoginSessionManager.invalidate})는 컨트롤러가 합니다.
 * 총괄의 {@code LoginService} 처럼 이 서비스는 DB 만 다루고 세션은 다루지 않습니다.
 *
 * <p>email_verifications 는 회원가입(ACC-001) 것을 {@code type = PASSWORD_RESET} 으로 그대로 씁니다.
 * 토큰은 UUID 라 맞힐 수 없어서 틀린 횟수 제한을 두지 않습니다(총괄 확인).
 * 이메일은 실제로 보내지 않고 서버 로그에 찍습니다(총괄 공지).
 */
@Service
@Transactional(readOnly = true)
public class PasswordResetService {

	private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

	/** 없는 아이디·이메일, 삭제된 계정. 기능 정의서 예외 처리 "이메일 미등록 시 안내". */
	public static final String NOT_REGISTERED = "등록되지 않은 아이디 또는 이메일입니다.";

	/** 없는 토큰, 또는 재요청으로 무효가 된 이전 토큰. */
	public static final String INVALID_LINK = "유효하지 않은 재설정 링크입니다. 다시 요청해 주세요.";

	/** 이미 비밀번호를 바꾼 토큰. */
	public static final String USED_LINK = "이미 사용된 재설정 링크입니다. 다시 요청해 주세요.";

	/** 30분이 지난 토큰. 기능 정의서 예외 처리 "링크 유효시간(30분) 초과 시 재요청 안내". */
	public static final String EXPIRED_LINK = "재설정 링크가 만료되었습니다(30분). 다시 요청해 주세요.";

	private final UserRepository userRepository;
	private final EmailVerificationRepository emailVerificationRepository;
	private final PasswordEncoder passwordEncoder;

	public PasswordResetService(
			UserRepository userRepository,
			EmailVerificationRepository emailVerificationRepository,
			PasswordEncoder passwordEncoder
	) {
		this.userRepository = userRepository;
		this.emailVerificationRepository = emailVerificationRepository;
		this.passwordEncoder = passwordEncoder;
	}

	/**
	 * 재설정 링크 요청. 계정이 있으면 그 계정 이메일로 토큰을 저장하고 서버 로그에 찍습니다.
	 * 다시 요청하면 새 행이 생기고, 가장 최근 토큰만 유효합니다(회원가입 인증코드와 같은 방식).
	 */
	@Transactional
	public void requestReset(PasswordResetRequest request) {
		User user = findActiveUser(request.loginIdOrEmail());

		String token = UUID.randomUUID().toString();
		LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(EmailVerification.PASSWORD_RESET_VALID_MINUTES);
		emailVerificationRepository.save(
				EmailVerification.create(user.getEmail(), token, VerificationType.PASSWORD_RESET, expiresAt));

		// 실제 메일 대신 서버 로그에 찍습니다(총괄 공지). 이 token 으로 POST /api/password-reset/confirm 을 부르면 됩니다.
		log.info("[비밀번호 재설정 링크] email={}, token={}, expiresAt={}", user.getEmail(), token, expiresAt);
	}

	/**
	 * 새 비밀번호 저장. 토큰이 없거나·이전 것이거나·사용됐거나·만료됐으면 BusinessException.
	 * 성공하면 비밀번호를 바꾸고 로그인 실패 횟수·잠금도 풀어 줍니다(총괄 확인).
	 *
	 * @return 비밀번호를 바꾼 사용자 ID. 컨트롤러가 이 사용자의 기존 세션을 끊는 데 씁니다
	 */
	@Transactional
	public Long resetPassword(PasswordResetConfirmRequest request) {
		EmailVerification verification = emailVerificationRepository
				.findByCodeAndType(request.token(), VerificationType.PASSWORD_RESET)
				.orElseThrow(() -> new BusinessException(INVALID_LINK));

		if (verification.isUsed()) {
			throw new BusinessException(USED_LINK);
		}
		if (verification.isExpired(LocalDateTime.now())) {
			throw new BusinessException(EXPIRED_LINK);
		}
		if (!isLatest(verification)) {
			throw new BusinessException(INVALID_LINK);
		}

		User user = userRepository.findByEmailAndStatus(verification.getEmail(), EntityStatus.ACTIVE)
				.orElseThrow(() -> new BusinessException(NOT_REGISTERED));

		user.changePassword(passwordEncoder.encode(request.newPassword()));
		user.resetLoginFailures();
		verification.markUsed();

		log.info("[비밀번호 재설정 완료] loginId={}", user.getLoginId());
		return user.getId();
	}

	/** 아이디 또는 이메일로 ACTIVE 계정 찾기. {@code @} 가 있으면 이메일로 봅니다. */
	private User findActiveUser(String loginIdOrEmail) {
		Optional<User> user = loginIdOrEmail.contains("@")
				? userRepository.findByEmailAndStatus(loginIdOrEmail, EntityStatus.ACTIVE)
				: userRepository.findByLoginIdAndStatus(loginIdOrEmail, EntityStatus.ACTIVE);
		return user.orElseThrow(() -> new BusinessException(NOT_REGISTERED));
	}

	/** 이 이메일로 보낸 가장 최근 재설정 기록인지. 다시 요청했다면 이전 토큰은 쓸 수 없습니다. */
	private boolean isLatest(EmailVerification verification) {
		return emailVerificationRepository
				.findTopByEmailAndTypeOrderByIdDesc(verification.getEmail(), VerificationType.PASSWORD_RESET)
				.map(latest -> latest.getId().equals(verification.getId()))
				.orElse(false);
	}
}
