package com.stayhub.account.service;

import com.stayhub.account.dto.LoginRequest;
import com.stayhub.account.dto.LoginUser;
import com.stayhub.account.entity.User;
import com.stayhub.account.repository.UserRepository;
import com.stayhub.common.BusinessException;
import com.stayhub.common.EntityStatus;
import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인 판정 (ACC-002). 세션은 다루지 않고 "이 아이디·비밀번호로 들어와도 되는가"만 정합니다.
 *
 * <p><b>이 파일이 하는 일</b> — 순서가 중요합니다.
 * <ol>
 *   <li>ACTIVE 계정만 찾습니다. 없거나 삭제된 계정이면 통합 메시지</li>
 *   <li>잠겨 있으면 <b>비밀번호를 보기 전에</b> 막습니다. 잠긴 동안은 실패 횟수를 올리지 않습니다</li>
 *   <li>비밀번호가 틀리면 {@code User.recordLoginFailure}. 그 실패로 잠겼으면(5회째) 잠금 메시지, 아니면 통합 메시지</li>
 *   <li>맞으면 {@code User.resetLoginFailures} 후 세션에 넣을 {@link LoginUser} 를 돌려줍니다</li>
 * </ol>
 * 세션 발급·계정당 1세션은 {@link LoginSessionManager} 가 맡습니다.
 *
 * <p><b>{@code noRollbackFor = BusinessException.class} 를 지우지 마세요.</b>
 * 비밀번호가 틀리면 실패 횟수를 올린 뒤 예외를 던집니다. 기본 설정이면 예외 때문에 트랜잭션이 롤백되어
 * 올린 횟수가 DB 에 저장되지 않고, 몇 번을 틀려도 잠기지 않습니다.
 */
@Service
@Transactional(readOnly = true)
public class LoginService {

	/** 없는 아이디·틀린 비밀번호·삭제된 계정 공통. 어느 쪽인지 알려주지 않습니다(보안). */
	public static final String INVALID_CREDENTIALS = "아이디 또는 비밀번호가 올바르지 않습니다.";

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public LoginService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	/**
	 * @param now 현재 시각. 테스트에서 "10분 뒤" 같은 시점을 넘기려고 파라미터로 받습니다
	 * @throws BusinessException 로그인 실패. 메시지를 그대로 화면에 보여주면 됩니다
	 */
	@Transactional(noRollbackFor = BusinessException.class)
	public LoginUser login(LoginRequest request, LocalDateTime now) {
		User user = userRepository.findByLoginIdAndStatus(request.loginId(), EntityStatus.ACTIVE)
				.orElseThrow(() -> new BusinessException(INVALID_CREDENTIALS));

		if (user.isLocked(now)) {
			throw new BusinessException(lockedMessage(user, now));
		}

		if (!passwordEncoder.matches(request.password(), user.getPassword())) {
			user.recordLoginFailure(now);
			// 이번 실패로 잠겼으면(5회째) 바로 잠금 안내 — 화면 흐름도 FLW-01
			throw new BusinessException(user.isLocked(now) ? lockedMessage(user, now) : INVALID_CREDENTIALS);
		}

		user.resetLoginFailures();
		return LoginUser.from(user);
	}

	/** "N분 후 다시 시도해 주세요." — 남은 시간을 분 단위로 올림. 예) 29분 1초 남음 → 30분 */
	private String lockedMessage(User user, LocalDateTime now) {
		long millis = Duration.between(now, user.getLockoutUntil()).toMillis();
		long minutes = (millis + 59_999) / 60_000;
		return minutes + "분 후 다시 시도해 주세요.";
	}
}
