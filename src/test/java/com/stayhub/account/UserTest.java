package com.stayhub.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.stayhub.account.entity.User;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 계정 잠금 규칙 테스트 (DB 없이 엔티티만).
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>로그인 실패 5회 → 30분 잠금, 성공 시 초기화 규칙(ACC-002)을 확인합니다.
 * 시각을 직접 넘기기 때문에 30분을 실제로 기다리지 않습니다. DB·Spring 없이 바로 돌아갑니다.
 *
 * <p><b>따라 만들 때 바꿀 곳</b>
 * <br>없습니다. 로그인 서비스를 만들 때 이 규칙은 다시 테스트하지 말고,
 * 서비스가 {@code isLocked} 를 먼저 확인하는지·성공 시 {@code resetLoginFailures} 를 부르는지만 테스트합니다.
 */
class UserTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 2, 10, 0);

	@Test
	@DisplayName("4회 실패는 잠기지 않고, 5회째 실패하면 30분 잠긴다")
	void locksOnFifthFailure() {
		User user = newUser();

		for (int i = 0; i < 4; i++) {
			user.recordLoginFailure(NOW);
		}
		assertThat(user.isLocked(NOW)).isFalse();
		assertThat(user.getFailedAttempts()).isEqualTo(4);

		user.recordLoginFailure(NOW);
		assertThat(user.isLocked(NOW)).isTrue();
		assertThat(user.getLockoutUntil()).isEqualTo(NOW.plusMinutes(30));
	}

	@Test
	@DisplayName("잠금 30분이 지나면 잠금이 풀린다")
	void unlocksAfterThirtyMinutes() {
		User user = lockedUser();

		assertThat(user.isLocked(NOW.plusMinutes(29))).isTrue();
		assertThat(user.isLocked(NOW.plusMinutes(30))).isFalse();
	}

	@Test
	@DisplayName("로그인 성공하면 실패 횟수가 0 이 되고 잠금도 지워진다")
	void resetClearsFailures() {
		User user = newUser();
		user.recordLoginFailure(NOW);
		user.recordLoginFailure(NOW);

		user.resetLoginFailures();

		assertThat(user.getFailedAttempts()).isZero();
		assertThat(user.getLockoutUntil()).isNull();
	}

	private User newUser() {
		return User.create("tester01", "encoded", "테스터", "010-0000-0000", "tester01@stayhub.test", null);
	}

	private User lockedUser() {
		User user = newUser();
		for (int i = 0; i < User.MAX_FAILED_ATTEMPTS; i++) {
			user.recordLoginFailure(NOW);
		}
		return user;
	}
}
