package com.stayhub.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 로그인 확인을 어느 주소에 적용할지 정합니다.
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>{@code /api/**} 전체에 {@link LoginCheckInterceptor} 를 붙이고, 로그인 전에 불러야 하는 주소만 뺍니다.
 * 화면 주소({@code /}, 정적 파일)는 아직 검사하지 않습니다. 관리자 화면이 생기면 화면용 처리(로그인 화면으로 이동)를 따로 추가합니다.
 *
 * <p><b>따라 만들 때 바꿀 곳</b>
 * <br>로그인 없이 열어야 하는 API 가 새로 생길 때만 {@code excludePathPatterns} 에 추가합니다.
 * 함부로 추가하면 그 API 는 누구나 부를 수 있게 되므로 총괄과 상의하세요.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new LoginCheckInterceptor())
				.addPathPatterns("/api/**")
				.excludePathPatterns(
						"/api/auth/login",
						"/api/signup/**", // /api/signup 자체도 포함됩니다
						"/api/password-reset/**" // 비밀번호를 잊은 사람이 부르므로 로그인 없이 (ACC-003, 총괄 확인)
						// 게스트(비로그인) API 가 생기면 여기에 추가 (예: 숙소 소개 SCR-GST-01, 비회원 예약 조회)
				);
	}
}
