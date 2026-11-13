package com.stayhub.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stayhub.account.dto.PasswordResetConfirmRequest;
import com.stayhub.account.dto.PasswordResetRequest;
import com.stayhub.account.entity.EmailVerification;
import com.stayhub.account.entity.User;
import com.stayhub.account.entity.VerificationType;
import com.stayhub.account.repository.EmailVerificationRepository;
import com.stayhub.account.repository.UserRepository;
import com.stayhub.account.service.PasswordResetService;
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
 * 비밀번호 찾기·재설정 서비스 테스트 (ACC-003).
 *
 * <p>{@code @Transactional} 이라 테스트가 끝나면 넣었던 데이터가 전부 되돌려집니다(롤백).
 * 토큰은 응답으로 주지 않기 때문에, 테스트에서는 저장된 기록을 리포지토리로 읽어서 씁니다.
 * 기존 세션 만료는 컨트롤러가 하므로 여기서는 확인하지 않습니다.
 */
@SpringBootTest
@Transactional
class PasswordResetServiceTest {

	private static final String LOGIN_ID = "reset01";
	private static final String EMAIL = "reset-test@test.com";
	private static final String OLD_PASSWORD = "old1234!";
	private static final String NEW_PASSWORD = "new1234!";

	@Autowired
	private PasswordResetService passwordResetService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EmailVerificationRepository emailVerificationRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private User user;

	@BeforeEach
	void setUp() {
		// 경고: @Transactional을 지우면 개발 DB의 이 테이블이 실제로 비워집니다. 지우지 마세요.
		emailVerificationRepository.deleteAll();
		user = userRepository.save(User.create(
				LOGIN_ID, passwordEncoder.encode(OLD_PASSWORD), "테스트", "010-1234-5678", EMAIL, null));
	}

	@Test
	@DisplayName("아이디로도, 이메일로도 요청하면 그 계정 이메일로 30분짜리 토큰이 저장된다")
	void requestByLoginIdOrEmailSavesToken() {
		passwordResetService.requestReset(new PasswordResetRequest(LOGIN_ID));
		EmailVerification byLoginId = latestReset();
		assertThat(byLoginId.getType()).isEqualTo(VerificationType.PASSWORD_RESET);
		assertThat(byLoginId.getExpiresAt())
				.isAfter(LocalDateTime.now().plusMinutes(29))
				.isBefore(LocalDateTime.now().plusMinutes(31));

		passwordResetService.requestReset(new PasswordResetRequest(EMAIL));
		assertThat(latestReset().getCode()).isNotEqualTo(byLoginId.getCode());
	}

	@Test
	@DisplayName("등록되지 않은 아이디·이메일이면 BusinessException 이 난다")
	void notRegisteredThrows() {
		assertThatThrownBy(() -> passwordResetService.requestReset(new PasswordResetRequest("nobody99")))
				.isInstanceOf(BusinessException.class)
				.hasMessage("등록되지 않은 아이디 또는 이메일입니다.");

		assertThatThrownBy(() -> passwordResetService.requestReset(new PasswordResetRequest("nobody@test.com")))
				.isInstanceOf(BusinessException.class)
				.hasMessage("등록되지 않은 아이디 또는 이메일입니다.");
	}

	@Test
	@DisplayName("재설정하면 새 비밀번호로 바뀌고, 로그인 잠금이 풀리고, 같은 링크는 다시 쓸 수 없다")
	void resetChangesPasswordAndUnlocks() {
		LocalDateTime now = LocalDateTime.now();
		for (int i = 0; i < User.MAX_FAILED_ATTEMPTS; i++) {
			user.recordLoginFailure(now); // 5번 틀려서 잠긴 상태로 만듦
		}
		assertThat(user.isLocked(now)).isTrue();

		passwordResetService.requestReset(new PasswordResetRequest(LOGIN_ID));
		String token = latestReset().getCode();

		Long userId = passwordResetService.resetPassword(new PasswordResetConfirmRequest(token, NEW_PASSWORD));

		User changed = userRepository.findById(userId).orElseThrow();
		assertThat(passwordEncoder.matches(NEW_PASSWORD, changed.getPassword())).isTrue();
		assertThat(passwordEncoder.matches(OLD_PASSWORD, changed.getPassword())).isFalse();
		assertThat(changed.isLocked(now)).isFalse();
		assertThat(changed.getFailedAttempts()).isZero();

		assertThatThrownBy(() -> passwordResetService.resetPassword(new PasswordResetConfirmRequest(token, "again1234!")))
				.isInstanceOf(BusinessException.class)
				.hasMessage("이미 사용된 재설정 링크입니다. 다시 요청해 주세요.");
	}

	@Test
	@DisplayName("30분이 지난 링크로 재설정하면 BusinessException 이 난다")
	void expiredTokenThrows() {
		// 만료 시각이 지난 기록을 직접 넣어 확인 (30분을 기다리지 않기 위해)
		emailVerificationRepository.save(EmailVerification.create(
				EMAIL, "expired-token", VerificationType.PASSWORD_RESET, LocalDateTime.now().minusMinutes(1)));

		assertThatThrownBy(() -> passwordResetService.resetPassword(new PasswordResetConfirmRequest("expired-token", NEW_PASSWORD)))
				.isInstanceOf(BusinessException.class)
				.hasMessage("재설정 링크가 만료되었습니다(30분). 다시 요청해 주세요.");
	}

	@Test
	@DisplayName("다시 요청하면 이전 링크는 쓸 수 없고 최신 링크만 통한다")
	void requestAgainInvalidatesOldToken() {
		passwordResetService.requestReset(new PasswordResetRequest(LOGIN_ID));
		String oldToken = latestReset().getCode();
		passwordResetService.requestReset(new PasswordResetRequest(LOGIN_ID));
		String newToken = latestReset().getCode();

		assertThatThrownBy(() -> passwordResetService.resetPassword(new PasswordResetConfirmRequest(oldToken, NEW_PASSWORD)))
				.isInstanceOf(BusinessException.class)
				.hasMessage("유효하지 않은 재설정 링크입니다. 다시 요청해 주세요.");

		passwordResetService.resetPassword(new PasswordResetConfirmRequest(newToken, NEW_PASSWORD));
	}

	/** 이 이메일로 가장 최근에 보낸 재설정 기록. (실제로는 서버 로그에서 보는 토큰) */
	private EmailVerification latestReset() {
		return emailVerificationRepository
				.findTopByEmailAndTypeOrderByIdDesc(EMAIL, VerificationType.PASSWORD_RESET)
				.orElseThrow();
	}
}
