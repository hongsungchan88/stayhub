package com.stayhub.account.service;

import com.stayhub.account.dto.EmailCodeSendRequest;
import com.stayhub.account.dto.EmailCodeVerifyRequest;
import com.stayhub.account.dto.SignupRequest;
import com.stayhub.account.dto.SignupResponse;
import com.stayhub.account.entity.EmailVerification;
import com.stayhub.account.entity.User;
import com.stayhub.account.entity.VerificationType;
import com.stayhub.account.repository.EmailVerificationRepository;
import com.stayhub.account.repository.UserRepository;
import com.stayhub.common.BusinessException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원가입 업무 로직 (ACC-001).
 *
 * <p><b>흐름</b> — 기능 정의서 순서대로 "인증 먼저, 저장은 나중" 입니다.
 * <ol>
 *   <li>아이디 중복 확인 — {@link #checkLoginId}</li>
 *   <li>인증코드 발송·재전송 — {@link #sendEmailCode} (email_verifications 에 저장, 서버 로그에 코드 출력)</li>
 *   <li>인증코드 확인 — {@link #verifyEmailCode} (최신 코드·만료·틀린 횟수 확인 후 is_used = true)</li>
 *   <li>회원가입 — {@link #signup} (확인된 인증이 있어야 저장. User.create + verifyEmail)</li>
 * </ol>
 *
 * <p>총괄의 User·UserRepository·PasswordEncoder 를 고치지 않고 그대로 씁니다.
 * 이메일은 실제로 보내지 않고 서버 로그에 찍습니다(총괄 공지).
 */
@Service
@Transactional(readOnly = true)
public class SignupService {

	private static final Logger log = LoggerFactory.getLogger(SignupService.class);

	/** 인증코드 생성용. 예측하기 어려운 난수를 씁니다. */
	private static final SecureRandom RANDOM = new SecureRandom();

	/** 인증코드를 5번 틀렸을 때. 그 코드는 무효이고 재전송해야 합니다. */
	public static final String TOO_MANY_ATTEMPTS = "인증코드를 5번 틀렸습니다. 인증코드를 다시 요청해 주세요.";

	private final UserRepository userRepository;
	private final EmailVerificationRepository emailVerificationRepository;
	private final PasswordEncoder passwordEncoder;

	public SignupService(
			UserRepository userRepository,
			EmailVerificationRepository emailVerificationRepository,
			PasswordEncoder passwordEncoder
	) {
		this.userRepository = userRepository;
		this.emailVerificationRepository = emailVerificationRepository;
		this.passwordEncoder = passwordEncoder;
	}

	/** 아이디 중복 확인. 이미 있으면 BusinessException (삭제된 계정 포함). */
	public void checkLoginId(String loginId) {
		if (userRepository.existsByLoginId(loginId)) {
			throw new BusinessException("이미 사용 중인 아이디입니다.");
		}
	}

	/**
	 * 인증코드 발송·재전송. 6자리 숫자 코드를 만들어 저장하고 서버 로그에 찍습니다.
	 * 재전송하면 새 행이 생기고, 확인할 때는 가장 최근 코드만 유효합니다.
	 */
	@Transactional
	public void sendEmailCode(EmailCodeSendRequest request) {
		if (userRepository.existsByEmail(request.email())) {
			throw new BusinessException("이미 사용 중인 이메일입니다.");
		}

		String code = String.format("%06d", RANDOM.nextInt(1_000_000));
		LocalDateTime expiresAt = LocalDateTime.now().plusHours(EmailVerification.JOIN_VALID_HOURS);
		emailVerificationRepository.save(
				EmailVerification.create(request.email(), code, VerificationType.JOIN, expiresAt));

		// 실제 메일 대신 서버 로그에 찍습니다(총괄 공지). 콘솔에서 이 줄을 보고 코드를 입력하면 됩니다.
		log.info("[회원가입 인증코드] email={}, code={}, expiresAt={}", request.email(), code, expiresAt);
	}

	/**
	 * 인증코드 확인. 가장 최근 코드와 비교하고, 맞으면 사용 완료로 표시합니다.
	 * 틀리면 틀린 횟수를 올리고, 5번째로 틀리면 그 코드는 더 이상 쓸 수 없습니다(재전송 필요).
	 *
	 * <p><b>{@code noRollbackFor = BusinessException.class} 를 지우지 마세요.</b>
	 * 틀린 횟수를 올린 뒤 예외를 던지므로, 기본 설정이면 롤백되어 횟수가 저장되지 않습니다.
	 * (총괄의 {@code LoginService.login} 과 같은 이유)
	 */
	@Transactional(noRollbackFor = BusinessException.class)
	public void verifyEmailCode(EmailCodeVerifyRequest request) {
		EmailVerification verification = emailVerificationRepository
				.findTopByEmailAndTypeOrderByIdDesc(request.email(), VerificationType.JOIN)
				.orElseThrow(() -> new BusinessException("인증코드를 먼저 요청해 주세요."));

		if (verification.isUsed()) {
			throw new BusinessException("이미 인증이 완료된 코드입니다.");
		}
		if (verification.isExpired(LocalDateTime.now())) {
			throw new BusinessException("인증코드가 만료되었습니다. 다시 요청해 주세요.");
		}
		if (verification.isAttemptsExceeded()) {
			throw new BusinessException(TOO_MANY_ATTEMPTS);
		}
		if (!verification.getCode().equals(request.code())) {
			verification.recordFailedAttempt();
			// 이번에 틀려서 한도에 닿았으면(5번째) 바로 재요청 안내
			throw new BusinessException(verification.isAttemptsExceeded() ? TOO_MANY_ATTEMPTS : "인증코드가 일치하지 않습니다.");
		}

		verification.markUsed();
	}

	/**
	 * 회원가입. 아이디·이메일 중복이거나 이메일 인증을 안 했으면 BusinessException.
	 * role 은 null 을 넘겨 기본값 STAFF 로 저장하고, 인증을 마친 상태라 verifyEmail() 로 is_verified = true.
	 */
	@Transactional
	public SignupResponse signup(SignupRequest request) {
		checkLoginId(request.loginId());
		if (userRepository.existsByEmail(request.email())) {
			throw new BusinessException("이미 사용 중인 이메일입니다.");
		}
		if (!emailVerificationRepository.existsByEmailAndTypeAndUsed(request.email(), VerificationType.JOIN, true)) {
			throw new BusinessException("이메일 인증을 먼저 완료해 주세요.");
		}

		User user = User.create(
				request.loginId(),
				passwordEncoder.encode(request.password()),
				request.name(),
				request.phoneNumber(),
				request.email(),
				null // STAFF (정의서 기본값)
		);
		user.verifyEmail();
		return SignupResponse.from(userRepository.save(user));
	}
}
