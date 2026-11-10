package com.stayhub.common;

import com.stayhub.account.dto.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * 관리자 화면({@code /admin/**}) 공통 처리. 어느 주소에 붙는지는 {@link WebConfig} 에서 정합니다.
 *
 * <p><b>이 파일이 하는 일</b>
 * <ul>
 *   <li>화면을 그리기 전 — 세션에 {@link LoginUser} 가 없으면 {@code /login?expired} 로 보냅니다.
 *       로그인 화면이 "세션이 만료되어 다시 로그인해 주세요." 를 띄웁니다(정책정의서 "세션 만료")</li>
 *   <li>화면을 그릴 때 — 레이아웃이 쓰는 {@code loginUser}(상단 이름·역할), {@code menuGroups}(좌측 메뉴),
 *       {@code currentUri}(지금 메뉴 강조)를 넣어 줍니다. 그래서 화면 컨트롤러는 화면 이름만 돌려주면 됩니다</li>
 * </ul>
 *
 * <p>{@code /api/**} 는 {@link LoginCheckInterceptor} 가 맡습니다. API 는 리다이렉트가 아니라 401 JSON 을
 * 돌려줘야 화면의 자바스크립트({@code stayhub.js} 의 {@code api})가 로그인 화면으로 보낼 수 있습니다.
 *
 * <p><b>따라 만들 때 바꿀 곳</b>
 * <br>없습니다. 화면마다 복사하지 마세요.
 */
public class AdminPageInterceptor implements HandlerInterceptor {

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws IOException {
		if (loginUser(request) == null) {
			response.sendRedirect(request.getContextPath() + "/login?expired");
			return false;
		}
		return true;
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) {
		LoginUser loginUser = loginUser(request);
		if (modelAndView == null || loginUser == null) {
			return; // 화면이 아닌 응답(리다이렉트 아님, 본문 직접 작성 등)
		}
		modelAndView.addObject("loginUser", loginUser);
		modelAndView.addObject("menuGroups", AdminMenu.groupsFor(loginUser.role()));
		modelAndView.addObject("currentUri", request.getRequestURI());
	}

	/** getSession(false) — 세션이 없을 때 새로 만들지 않습니다. */
	private LoginUser loginUser(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		return session == null ? null : (LoginUser) session.getAttribute(LoginUser.SESSION_KEY);
	}
}
