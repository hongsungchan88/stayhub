package com.stayhub.common;

/**
 * 업무 규칙을 어겼을 때 던지는 예외.
 *
 * <p>"이미 예약된 객실입니다", "취소할 수 없는 상태입니다" 처럼
 * 사용자에게 사유를 그대로 보여줄 수 있는 상황에 씁니다.
 * 여기 담은 메시지는 {@link GlobalExceptionHandler} 를 거쳐 화면까지 그대로 전달되니,
 * 개발자만 알아볼 수 있는 내용 대신 사용자가 읽을 문장을 넣으세요.
 *
 * <p>NullPointerException 같은 프로그램 버그에는 쓰지 않습니다.
 *
 * <p>사용 예시
 * <pre>
 * throw new BusinessException("이미 예약된 객실입니다.");
 * </pre>
 */
public class BusinessException extends RuntimeException {

	public BusinessException(String message) {
		super(message);
	}

	public BusinessException(String message, Throwable cause) {
		super(message, cause);
	}
}
