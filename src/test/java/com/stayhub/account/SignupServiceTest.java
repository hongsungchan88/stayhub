package com.stayhub.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stayhub.account.dto.EmailCodeSendRequest;
import com.stayhub.account.dto.EmailCodeVerifyRequest;
import com.stayhub.account.dto.SignupRequest;
import com.stayhub.account.dto.SignupResponse;
import com.stayhub.account.entity.EmailVerification;
import com.stayhub.account.entity.User;
import com.stayhub.account.entity.UserRole;
import com.stayhub.account.entity.VerificationType;
import com.stayhub.account.repository.EmailVerificationRepository;
import com.stayhub.account.repository.UserRepository;
import com.stayhub.account.service.SignupService;
import com.stayhub.common.BusinessException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원가입 서비스 테스트.
 *
 * <p>{@code @Transactional} 이라 테스트가 끝나면 넣었던 데이터가 전부 되돌려집니다(롤백).
 * 인증코드는 응답으로 주지 않기 때문에, 테스트에서는 저장된 기록을 리포지토리로 읽어서 씁니다.
 */
@SpringBootTest
@Transactional
class SignupServiceTest {

	private static final String EMAIL = "signup-test@test.com";

	@Autowired
	private SignupService signupService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EmailVerificationRepository emailVerificationRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void setUp() {
		// 경고: @Transactional을 지우면 개발 DB의 이 테이블이 실제로 비워집니다. 지우지 마세요.
		emailVerificationRepository.deleteAll();
	}

	@Test
	@DisplayName("인증코드 발송 → 확인 → 가입하면 저장되고, 인증 완료·STAFF·암호화된 비밀번호로 들어간다")
	void signupAfterVerification() {
		signupService.sendEmailCode(new EmailCodeSendRequest(EMAIL));
		signupService.verifyEmailCode(new EmailCodeVerifyRequest(EMAIL, latestCode()));

		SignupResponse created = signupService.signup(signupRequest("signup01", EMAIL));

		assertThat(created.verified()).isTrue();
		assertThat(created.role()).isEqualTo(UserRole.STAFF);

		User saved = userRepository.findById(created.id()).orElseThrow();
		assertThat(saved.getPassword()).isNotEqualTo("test1234!"); // 원문 그대로 저장되지 않음
		assertThat(passwordEncoder.matches("test1234!", saved.getPassword())).isTrue();
	}

	@Test
	@DisplayName("이메일 인증 없이 가입하면 BusinessException 이 난다")
	void signupWithoutVerificationThrows() {
		signupService.sendEmailCode(new EmailCodeSendRequest(EMAIL)); // 발송만 하고 확인은 안 함

		assertThatThrownBy(() -> signupService.signup(signupRequest("signup01", EMAIL)))
				.isInstanceOf(BusinessException.class)
				.hasMessage("이메일 인증을 먼저 완료해 주세요.");
	}

	@Test
	@DisplayName("틀린 코드·만료된 코드로 확인하면 BusinessException 이 난다")
	void wrongOrExpiredCodeThrows() {
		signupService.sendEmailCode(new EmailCodeSendRequest(EMAIL));
		String wrongCode = latestCode().equals("000000") ? "111111" : "000000";

		assertThatThrownBy(() -> signupService.verifyEmailCode(new EmailCodeVerifyRequest(EMAIL, wrongCode)))
				.isInstanceOf(BusinessException.class)
				.hasMessage("인증코드가 일치하지 않습니다.");

		// 만료 시각이 지난 기록을 직접 넣어 확인 (24시간을 기다리지 않기 위해)
		emailVerificationRepository.save(EmailVerification.create(
				EMAIL, "123456", VerificationType.JOIN, LocalDateTime.now().minusMinutes(1)));

		assertThatThrownBy(() -> signupService.verifyEmailCode(new EmailCodeVerifyRequest(EMAIL, "123456")))
				.isInstanceOf(BusinessException.class)
				.hasMessage("인증코드가 만료되었습니다. 다시 요청해 주세요.");
	}

	@Test
	@DisplayName("재전송하면 이전 코드는 쓸 수 없고 최신 코드만 통한다")
	void resendInvalidatesOldCode() {
		signupService.sendEmailCode(new EmailCodeSendRequest(EMAIL));
		String oldCode = latestCode();
		signupService.sendEmailCode(new EmailCodeSendRequest(EMAIL));
		String newCode = latestCode();

		if (!oldCode.equals(newCode)) { // 우연히 같은 숫자가 나온 경우는 건너뜀
			assertThatThrownBy(() -> signupService.verifyEmailCode(new EmailCodeVerifyRequest(EMAIL, oldCode)))
					.isInstanceOf(BusinessException.class)
					.hasMessage("인증코드가 일치하지 않습니다.");
		}
		signupService.verifyEmailCode(new EmailCodeVerifyRequest(EMAIL, newCode));
	}

	@Test
	@DisplayName("이미 있는 아이디는 중복 확인·가입에서 BusinessException 이 난다")
	void duplicateLoginIdThrows() {
		signupService.sendEmailCode(new EmailCodeSendRequest(EMAIL));
		signupService.verifyEmailCode(new EmailCodeVerifyRequest(EMAIL, latestCode()));
		signupService.signup(signupRequest("signup01", EMAIL));

		assertThatThrownBy(() -> signupService.checkLoginId("signup01"))
				.isInstanceOf(BusinessException.class)
				.hasMessage("이미 사용 중인 아이디입니다.");

		assertThatThrownBy(() -> signupService.signup(signupRequest("signup01", "other@test.com")))
				.isInstanceOf(BusinessException.class)
				.hasMessage("이미 사용 중인 아이디입니다.");
	}

	/** 이 이메일로 가장 최근에 보낸 인증코드. (실제로는 서버 로그에서 보는 값) */
	private String latestCode() {
		return emailVerificationRepository
				.findTopByEmailAndTypeOrderByIdDesc(EMAIL, VerificationType.JOIN)
				.orElseThrow()
				.getCode();
	}

	/** 테스트용 가입 요청. 비밀번호·이름·전화번호는 고정값입니다. */
	private SignupRequest signupRequest(String loginId, String email) {
		return new SignupRequest(loginId, "test1234!", "테스트", "010-1234-5678", email);
	}
}
