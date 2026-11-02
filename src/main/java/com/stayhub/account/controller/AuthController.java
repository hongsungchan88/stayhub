package com.stayhub.account.controller;

import com.stayhub.account.dto.LoginRequest;
import com.stayhub.account.dto.LoginUser;
import com.stayhub.account.service.LoginService;
import com.stayhub.account.service.LoginSessionManager;
import com.stayhub.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;

/**
 * 로그인·로그아웃 API (ACC-002·004).
 *
 * <p><b>이 파일이 하는 일</b>
 * <pre>
 * POST /api/auth/login    로그인. 성공하면 새 세션 발급(응답의 Set-Cookie: JSESSIONID)
 * POST /api/auth/logout   로그아웃. 세션 무효화 + JSESSIONID 쿠키 삭제
 * GET  /api/auth/me       현재 로그인 사용자 (화면 상단의 이름·역할 표시용)
 * </pre>
 * 로그인 없이 부를 수 있는 것은 login 뿐입니다. logout·me 는 로그인이 필요합니다(아니면 401).
 * 사용법은 docs/troubleshooting.md 의 "401 로그인이 필요합니다" 를 보세요.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	/** Tomcat 기본 세션 쿠키 이름. application.yml 에서 이름을 바꾸면 여기도 바꿔야 합니다. */
	private static final String SESSION_COOKIE = "JSESSIONID";

	private final LoginService loginService;
	private final LoginSessionManager loginSessionManager;

	public AuthController(LoginService loginService, LoginSessionManager loginSessionManager) {
		this.loginService = loginService;
		this.loginSessionManager = loginSessionManager;
	}

	@PostMapping("/login")
	public ApiResponse<LoginUser> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
		LoginUser loginUser = loginService.login(request, LocalDateTime.now());
		loginSessionManager.start(httpRequest, loginUser);
		return ApiResponse.success(loginUser);
	}

	@PostMapping("/logout")
	public ApiResponse<Void> logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
		HttpSession session = httpRequest.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		// 세션은 서버에서 끝났지만 브라우저에 쿠키가 남아 있으므로 Max-Age 0 으로 지우라고 알려줍니다.
		ResponseCookie expired = ResponseCookie.from(SESSION_COOKIE, "")
				.path("/")
				.maxAge(0)
				.httpOnly(true)
				.sameSite("Lax")
				.build();
		httpResponse.addHeader(HttpHeaders.SET_COOKIE, expired.toString());
		return ApiResponse.success(null);
	}

	@GetMapping("/me")
	public ApiResponse<LoginUser> me(@SessionAttribute(LoginUser.SESSION_KEY) LoginUser loginUser) {
		return ApiResponse.success(loginUser);
	}
}
