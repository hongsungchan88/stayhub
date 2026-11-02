package com.stayhub.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stayhub.account.dto.LoginRequest;
import com.stayhub.account.dto.LoginUser;
import com.stayhub.account.entity.User;
import com.stayhub.account.entity.UserRole;
import com.stayhub.account.repository.UserRepository;
import com.stayhub.account.service.LoginService;
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
 * 로그인 판정 테스트 (ACC-002).
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>실제 DB 에 계정을 하나 만들고 성공·실패·잠금·삭제된 계정을 확인합니다.
 * 시각({@code NOW})을 직접 넘겨서 "10분 뒤" 를 기다리지 않고 확인합니다.
 * {@code @Transactional} 이라 테스트가 끝나면 넣은 계정은 롤백됩니다.
 * 세션 발급·401 은 {@link AuthApiTest} 에서 확인합니다.
 */
@SpringBootTest
@Transactional
class LoginServiceTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 2, 10, 0);
	private static final String LOGIN_ID = "logintest01";
	private static final String PASSWORD = "pass1234!";

	@Autowired
	private LoginService loginService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private User user;

	@BeforeEach
	void setUp() {
		user = userRepository.save(User.create(
				LOGIN_ID, passwordEncoder.encode(PASSWORD), "로그인테스터", "010-0000-0000",
				"logintest01@test.com", UserRole.FRONT));
	}

	@Test
	@DisplayName("성공하면 실패 횟수가 0 이 되고, 세션에 넣을 사용자 정보가 나온다")
	void successResetsFailures() {
		failWrongPassword(NOW);
		failWrongPassword(NOW);
		assertThat(user.getFailedAttempts()).isEqualTo(2);

		LoginUser loginUser = loginService.login(new LoginRequest(LOGIN_ID, PASSWORD), NOW);

		assertThat(user.getFailedAttempts()).isZero();
		assertThat(loginUser).isEqualTo(new LoginUser(user.getId(), LOGIN_ID, "로그인테스터", UserRole.FRONT));
	}

	@Test
	@DisplayName("틀리면 횟수가 오르고, 5회째에 잠기며 \"30분 후\" 안내가 나간다")
	void fifthFailureLocks() {
		for (int i = 1; i <= 4; i++) {
			assertThat(failWrongPassword(NOW)).isEqualTo(LoginService.INVALID_CREDENTIALS);
			assertThat(user.getFailedAttempts()).isEqualTo(i);
		}

		assertThat(failWrongPassword(NOW)).isEqualTo("30분 후 다시 시도해 주세요.");
		assertThat(user.isLocked(NOW)).isTrue();
	}

	@Test
	@DisplayName("잠긴 동안은 맞는 비밀번호도 거부하고 남은 분을 알려주며, 실패 횟수는 오르지 않는다")
	void lockedRejectsCorrectPassword() {
		for (int i = 0; i < User.MAX_FAILED_ATTEMPTS; i++) {
			failWrongPassword(NOW);
		}
		int attemptsAfterLock = user.getFailedAttempts();

		// 10분 30초 뒤 → 남은 19분 30초 → 올림해서 20분
		LocalDateTime later = NOW.plusMinutes(10).plusSeconds(30);
		assertThatThrownBy(() -> loginService.login(new LoginRequest(LOGIN_ID, PASSWORD), later))
				.isInstanceOf(BusinessException.class)
				.hasMessage("20분 후 다시 시도해 주세요.");
		assertThat(failWrongPassword(later)).isEqualTo("20분 후 다시 시도해 주세요.");
		assertThat(user.getFailedAttempts()).isEqualTo(attemptsAfterLock);

		// 30분이 지나면 다시 로그인된다
		assertThat(loginService.login(new LoginRequest(LOGIN_ID, PASSWORD), NOW.plusMinutes(30)).userId())
				.isEqualTo(user.getId());
	}

	@Test
	@DisplayName("없는 아이디와 틀린 비밀번호는 같은 메시지다")
	void sameMessageForUnknownIdAndWrongPassword() {
		assertThatThrownBy(() -> loginService.login(new LoginRequest("nobody999", PASSWORD), NOW))
				.isInstanceOf(BusinessException.class)
				.hasMessage(LoginService.INVALID_CREDENTIALS);
		assertThat(failWrongPassword(NOW)).isEqualTo(LoginService.INVALID_CREDENTIALS);
	}

	@Test
	@DisplayName("삭제된(DELETED) 계정은 맞는 비밀번호로도 로그인할 수 없다")
	void deletedUserCannotLogin() {
		user.delete();
		userRepository.flush();

		assertThatThrownBy(() -> loginService.login(new LoginRequest(LOGIN_ID, PASSWORD), NOW))
				.isInstanceOf(BusinessException.class)
				.hasMessage(LoginService.INVALID_CREDENTIALS);
	}

	/** 틀린 비밀번호로 로그인해 보고, 나온 메시지를 돌려줍니다. */
	private String failWrongPassword(LocalDateTime now) {
		try {
			loginService.login(new LoginRequest(LOGIN_ID, "wrong1234!"), now);
		} catch (BusinessException e) {
			return e.getMessage();
		}
		throw new AssertionError("틀린 비밀번호인데 로그인됐습니다.");
	}
}
