package com.stayhub.common;

/**
 * 로그인은 했지만 권한이 없는 요청. {@link GlobalExceptionHandler} 가 403 으로 바꿉니다.
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>예) 호스트가 아닌 직원이 직원 계정 관리 API(ACC-005)를 호출했을 때 컨트롤러가 던집니다.
 * 로그인 안 함({@link UnauthorizedException}, 401)·업무 규칙 위반({@link BusinessException}, 400)과
 * 응답 코드를 나누려고 따로 둡니다. 화면은 403 을 받으면 "권한이 없습니다." 를 보여주면 됩니다.
 *
 * <p><b>따라 만들 때 바꿀 곳</b>
 * <br>없습니다. 던질 때는 {@code new ForbiddenException("권한이 없습니다.")} 처럼 메시지를 넘깁니다.
 */
public class ForbiddenException extends RuntimeException {

	public ForbiddenException(String message) {
		super(message);
	}
}
