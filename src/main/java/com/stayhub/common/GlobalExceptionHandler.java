package com.stayhub.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 컨트롤러에서 터진 예외를 한곳에서 받아 처리합니다.
 *
 * <p>각자 try-catch 를 흩뿌리지 않아도 되도록, 예외가 밖으로 던져지면
 * 여기서 잡아 {@link ApiResponse#error(String)} 형식으로 바꿔 응답합니다.
 * 덕분에 프런트는 실패 응답 모양을 하나만 알면 됩니다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	/**
	 * 업무 규칙 위반. 개발자가 의도적으로 던진 예외이므로
	 * 메시지를 그대로 사용자에게 보여줍니다.
	 */
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
		log.warn("업무 예외: {}", e.getMessage());
		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.error(e.getMessage()));
	}

	/**
	 * {@code @Valid} 검증 실패. 어느 필드가 왜 틀렸는지 함께 알려줍니다.
	 * 예) "email: 올바른 이메일 형식이 아닙니다"
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
		String message = e.getBindingResult().getFieldErrors().stream()
				.map(this::formatFieldError)
				.reduce((a, b) -> a + ", " + b)
				.orElse("입력값이 올바르지 않습니다.");

		log.warn("검증 실패: {}", message);
		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.error(message));
	}

	/**
	 * 위에서 걸러지지 않은 나머지 전부. 대개 코드 버그입니다.
	 *
	 * <p>원인은 서버 로그에만 남기고 사용자에게는 일반적인 문장만 보여줍니다.
	 * 예외 메시지를 그대로 내보내면 내부 구조가 드러날 수 있기 때문입니다.
	 * 개발 중에는 콘솔에 찍힌 스택 트레이스를 보고 원인을 찾으세요.
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
		log.error("처리하지 못한 예외", e);
		return ResponseEntity
				.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.error("서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."));
	}

	private String formatFieldError(FieldError error) {
		return error.getField() + ": " + error.getDefaultMessage();
	}
}
