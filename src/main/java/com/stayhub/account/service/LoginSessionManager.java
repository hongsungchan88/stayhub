package com.stayhub.account.service;

import com.stayhub.account.dto.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * 로그인 세션 발급과 계정당 1세션 관리 (ACC-002, 요구사항 정의서 "세션 관리").
 *
 * <p><b>이 파일이 하는 일</b>
 * <ul>
 *   <li>{@link #start} — 기존 세션을 버리고 새 세션을 만들어 {@link LoginUser} 를 넣습니다(세션 고정 공격 방지).
 *       같은 계정이 이미 다른 곳에서 로그인해 있으면 그 세션을 끊습니다(계정당 1세션)</li>
 *   <li>{@link #invalidate} — 특정 사용자의 세션을 끊습니다(비밀번호 재설정 후 기존 세션 만료, ACC-003)</li>
 *   <li>{@link #sessionDestroyed} — 로그아웃·30분 만료로 세션이 없어지면 목록에서 지웁니다</li>
 * </ul>
 *
 * <p><b>왜 맵 하나로 충분한가</b>
 * <br>정책정의서 "인증 방식": 서버 1대, 인메모리 세션. 서버가 여러 대가 되면 이 맵이 서버마다 따로 생겨서
 * 계정당 1세션이 깨집니다. 그때는 Spring Session(Redis 등)으로 바꿔야 합니다.
 *
 * <p>{@code HttpSessionListener} 를 구현한 빈은 Spring Boot 가 서버(Tomcat)에 자동으로 등록합니다.
 */
@Component
public class LoginSessionManager implements HttpSessionListener {

	/** userId → 그 계정의 현재 세션 */
	private final Map<Long, HttpSession> sessions = new ConcurrentHashMap<>();

	/** 로그인 성공 후 컨트롤러가 부릅니다. */
	public void start(HttpServletRequest request, LoginUser loginUser) {
		HttpSession oldSession = request.getSession(false);
		if (oldSession != null) {
			oldSession.invalidate();
		}
		HttpSession newSession = request.getSession(true);
		newSession.setAttribute(LoginUser.SESSION_KEY, loginUser);

		HttpSession previous = sessions.put(loginUser.userId(), newSession);
		if (previous != null && previous != newSession) {
			invalidateQuietly(previous);
		}
	}

	/**
	 * 이 사용자의 세션을 끊습니다. 로그인해 있지 않으면 아무 일도 하지 않습니다.
	 * 비밀번호 재설정(ACC-003)이 끝난 뒤 부르세요. 끊긴 세션으로 요청하면 401 이 납니다.
	 */
	public void invalidate(Long userId) {
		HttpSession session = sessions.remove(userId);
		if (session != null) {
			invalidateQuietly(session);
		}
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent event) {
		HttpSession destroyed = event.getSession();
		if (destroyed.getAttribute(LoginUser.SESSION_KEY) instanceof LoginUser loginUser) {
			// 같은 계정이 새로 로그인해 맵이 이미 새 세션을 가리키면 지우지 않습니다.
			sessions.computeIfPresent(loginUser.userId(),
					(userId, current) -> current.getId().equals(destroyed.getId()) ? null : current);
		}
	}

	/** 이미 만료·로그아웃된 세션이면 invalidate 가 예외를 던지는데, 목적은 이미 이뤄졌으니 무시합니다. */
	private void invalidateQuietly(HttpSession session) {
		try {
			session.invalidate();
		} catch (IllegalStateException alreadyInvalidated) {
			// 이미 끊긴 세션
		}
	}
}
