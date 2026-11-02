package com.stayhub.common;

/**
 * 로그인하지 않은 요청. {@link GlobalExceptionHandler} 가 401 로 바꿉니다.
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>{@link LoginCheckInterceptor} 가 세션에 로그인 정보가 없을 때 던집니다.
 * 업무 규칙 위반({@link BusinessException}, 400)과 응답 코드를 나누려고 따로 둡니다.
 * 화면은 401 을 받으면 로그인 화면으로 보내면 됩니다.
 *
 * <p><b>따라 만들 때 바꿀 곳</b>
 * <br>없습니다. 서비스·컨트롤러에서 직접 던질 일은 보통 없습니다.
 */
public class UnauthorizedException extends RuntimeException {

	public UnauthorizedException(String message) {
		super(message);
	}
}
