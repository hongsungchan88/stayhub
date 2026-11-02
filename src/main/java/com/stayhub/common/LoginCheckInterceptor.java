package com.stayhub.common;

import com.stayhub.account.dto.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 로그인했는지 확인하는 문지기. 어느 주소에 붙는지는 {@link WebConfig} 에서 정합니다.
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>컨트롤러가 실행되기 전에 세션에 {@link LoginUser} 가 있는지 봅니다.
 * 없으면 {@link UnauthorizedException} 을 던지고, {@link GlobalExceptionHandler} 가
 * 401 + "로그인이 필요합니다." 로 응답합니다. 있으면 그냥 통과시킵니다.
 *
 * <p>여기서는 "로그인했는가"만 봅니다. 역할(HOST·FRONT 등)별로 막는 것(403)은 ACC-005 에서 따로 추가합니다.
 *
 * <p>{@code getSession(false)} — 세션이 없을 때 새로 만들지 않습니다.
 * {@code true} 로 쓰면 로그인하지 않은 요청마다 빈 세션이 생겨 메모리를 차지합니다.
 */
public class LoginCheckInterceptor implements HandlerInterceptor {

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		HttpSession session = request.getSession(false);
		if (session == null || session.getAttribute(LoginUser.SESSION_KEY) == null) {
			throw new UnauthorizedException("로그인이 필요합니다.");
		}
		return true;
	}
}
