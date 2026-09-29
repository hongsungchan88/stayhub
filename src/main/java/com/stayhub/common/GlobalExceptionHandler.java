package com.stayhub.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 컨트롤러에서 터진 예외를 한곳에서 받아 처리합니다.
 *
 * <p>각자 try-catch 를 흩뿌리지 않아도 되도록, 예외가 밖으로 던져지면
 * 여기서 잡아 {@link ApiResponse#error(String)} 형식으로 바꿔 응답합니다.
 * 덕분에 프런트는 실패 응답 모양을 하나만 알면 됩니다.
 *
 * <p>응답 코드
 * <ul>
 *   <li>400 — 요청이 잘못됨 (업무 규칙 위반, 검증 실패, 값 형식 오류, JSON 오류, 필수 파라미터 누락)</li>
 *   <li>404 — 없는 주소 (favicon.ico 등). ERROR 로그를 남기지 않습니다</li>
 *   <li>405 — 주소는 맞는데 그 주소가 받지 않는 요청 방식 (예: DELETE API 가 없는 테이블에 DELETE)</li>
 *   <li>415 — 요청 본문 형식이 JSON 이 아님 (Content-Type 누락 등)</li>
 *   <li>500 — 위에 해당하지 않는 나머지. 대개 코드 버그이고 ERROR 로그와 스택 트레이스를 남깁니다</li>
 * </ul>
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
	 * 주소나 쿼리 파라미터의 값을 원하는 타입으로 바꿀 수 없을 때. 400.
	 *
	 * <p>이런 요청일 때 납니다.
	 * <ul>
	 *   <li>{@code GET /api/guidebooks?category=wifi} — enum 은 대문자 이름만 받습니다 ({@code WIFI})</li>
	 *   <li>{@code GET /api/guidebooks?category=POOL} — enum 에 없는 값</li>
	 *   <li>{@code GET /api/guidebooks/abc} — 숫자 ID 자리에 글자</li>
	 *   <li>{@code GET /api/guidebooks?page=first} — 숫자 파라미터에 글자</li>
	 * </ul>
	 * 응답 예) "category 값이 올바르지 않습니다."
	 */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
		log.warn("파라미터 형식 오류: {}={}", e.getName(), e.getValue());
		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.error(e.getName() + " 값이 올바르지 않습니다."));
	}

	/**
	 * 요청 본문(JSON)을 읽을 수 없을 때. 400.
	 *
	 * <p>이런 요청일 때 납니다.
	 * <ul>
	 *   <li>JSON 문법 오류 — 중괄호·따옴표·쉼표가 빠지거나 남음</li>
	 *   <li>enum 에 없는 값 — 예) {@code "category": "POOL"}, {@code "category": "wifi"}</li>
	 *   <li>타입이 다른 값 — 예) {@code "sortOrder": "첫번째"}</li>
	 *   <li>POST·PUT 인데 본문이 비어 있음</li>
	 * </ul>
	 * 어느 필드가 틀렸는지는 서버 로그(WARN)에 남습니다. 응답은 "요청 내용을 읽을 수 없습니다. 입력 형식을 확인해 주세요."
	 */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException e) {
		log.warn("요청 본문 읽기 실패: {}", e.getMostSpecificCause().getMessage());
		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.error("요청 내용을 읽을 수 없습니다. 입력 형식을 확인해 주세요."));
	}

	/**
	 * 꼭 필요한 쿼리 파라미터가 빠졌을 때. 400.
	 *
	 * <p>{@code @RequestParam} 은 {@code required = false} 나 {@code defaultValue} 를 주지 않으면 필수입니다.
	 * 그런 파라미터 없이 호출하면 납니다. 예) 기간 조회 API 를 {@code ?from=} 없이 호출
	 * (가이드북 API 는 파라미터가 모두 선택이라 이 오류가 나지 않습니다.)
	 * 응답 예) "from 값이 필요합니다."
	 */
	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ApiResponse<Void>> handleMissingParameter(MissingServletRequestParameterException e) {
		log.warn("필수 파라미터 누락: {}", e.getParameterName());
		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.error(e.getParameterName() + " 값이 필요합니다."));
	}

	/**
	 * 주소는 맞는데 그 주소가 받지 않는 요청 방식(GET·POST·PUT·DELETE)으로 보냈을 때. 405.
	 *
	 * <p>이런 요청일 때 납니다.
	 * <ul>
	 *   <li>유형 ②·③ 테이블에 DELETE 요청 — 이 테이블들은 DELETE API 가 없습니다.
	 *       예) {@code curl -X DELETE http://localhost:8080/api/rooms/1}</li>
	 *   <li>목록 주소에 PUT·DELETE — 예) {@code DELETE /api/guidebooks} (ID 빠짐)</li>
	 * </ul>
	 * 응답은 "지원하지 않는 요청 방식입니다."
	 */
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
		log.warn("지원하지 않는 요청 방식: {}", e.getMethod());
		return ResponseEntity
				.status(HttpStatus.METHOD_NOT_ALLOWED)
				.body(ApiResponse.error("지원하지 않는 요청 방식입니다."));
	}

	/**
	 * 요청 본문의 형식(Content-Type)이 JSON 이 아닐 때. 415.
	 *
	 * <p>이런 요청일 때 납니다.
	 * <ul>
	 *   <li>curl 로 POST·PUT 하면서 {@code -H "Content-Type: application/json"} 을 빠뜨림
	 *       — curl 은 이때 본문을 폼 형식(x-www-form-urlencoded)으로 보냅니다</li>
	 *   <li>화면 코드에서 fetch 로 보낼 때 headers 에 Content-Type 을 안 넣음</li>
	 * </ul>
	 * 응답은 "요청 형식이 올바르지 않습니다. Content-Type: application/json 을 확인해 주세요."
	 */
	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
		log.warn("지원하지 않는 요청 형식: {}", e.getContentType());
		return ResponseEntity
				.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
				.body(ApiResponse.error("요청 형식이 올바르지 않습니다. Content-Type: application/json 을 확인해 주세요."));
	}

	/**
	 * 없는 주소를 요청했을 때. 404.
	 *
	 * <p>이런 요청일 때 납니다.
	 * <ul>
	 *   <li>{@code /favicon.ico} — 브라우저가 화면을 열 때마다 자동으로 요청합니다</li>
	 *   <li>주소 오타 — 예) {@code /api/guidebook} (s 빠짐)</li>
	 * </ul>
	 * 코드 버그가 아니라 흔히 일어나는 일이라 ERROR 로그를 남기지 않습니다.
	 * 남기면 브라우저로 localhost:8080 을 열 때마다 콘솔에 빨간 스택 트레이스가 찍힙니다.
	 * 응답은 "요청한 주소를 찾을 수 없습니다."
	 */
	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException e) {
		log.debug("없는 주소 요청: {}", e.getResourcePath());
		return ResponseEntity
				.status(HttpStatus.NOT_FOUND)
				.body(ApiResponse.error("요청한 주소를 찾을 수 없습니다."));
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
