package com.stayhub.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 예외 처리 테스트.
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>잘못된 요청마다 {@link GlobalExceptionHandler} 가 맞는 응답 코드(400·404·500)와
 * {@link ApiResponse} 실패 형식으로 답하는지 확인합니다.
 *
 * <p>DB·서버를 띄우지 않습니다. 테스트 전용 컨트롤러({@link TestController})로 예외를 일부러 일으키고,
 * 예외 처리기만 붙인 가짜 요청 환경(MockMvc)으로 호출합니다. 그래서 MySQL 없이도 돌아갑니다.
 *
 * <p><b>따라 만들 때 바꿀 곳</b>
 * <br>없습니다. 공통 파일의 테스트라 도메인 기능을 만들 때 복사하지 않습니다.
 * 예외 처리기를 새로 추가하면 여기에 경우를 하나 더하세요.
 */
@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	@Test
	@DisplayName("쿼리 파라미터 값이 enum 에 없으면 400 — ?category=wifi")
	void enumParamTypeMismatch() throws Exception {
		mockMvc.perform(get("/test/category").param("category", "wifi"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.data").doesNotExist())
				.andExpect(jsonPath("$.message").value("category 값이 올바르지 않습니다."));
	}

	@Test
	@DisplayName("숫자 ID 자리에 글자가 오면 400 — /abc")
	void pathVariableTypeMismatch() throws Exception {
		mockMvc.perform(get("/test/items/abc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("id 값이 올바르지 않습니다."));
	}

	@Test
	@DisplayName("JSON 문법이 틀리면 400")
	void malformedJson() throws Exception {
		mockMvc.perform(post("/test/body").contentType(MediaType.APPLICATION_JSON).content("{\"category\":\"WIFI\","))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("요청 내용을 읽을 수 없습니다. 입력 형식을 확인해 주세요."));
	}

	@Test
	@DisplayName("JSON 본문의 enum 값이 없는 값이면 400 — \"category\":\"POOL\"")
	void unknownEnumInBody() throws Exception {
		mockMvc.perform(post("/test/body").contentType(MediaType.APPLICATION_JSON).content("{\"category\":\"POOL\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("요청 내용을 읽을 수 없습니다. 입력 형식을 확인해 주세요."));
	}

	@Test
	@DisplayName("필수 쿼리 파라미터가 빠지면 400")
	void missingRequiredParam() throws Exception {
		mockMvc.perform(get("/test/required"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("from 값이 필요합니다."));
	}

	@Test
	@DisplayName("없는 주소는 404 이고 ERROR 로그를 남기지 않는다 — favicon.ico")
	void noResourceIs404WithoutErrorLog(CapturedOutput output) throws Exception {
		mockMvc.perform(get("/test/no-resource"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("요청한 주소를 찾을 수 없습니다."));

		assertThat(output).doesNotContain("처리하지 못한 예외");
		assertThat(output).doesNotContain("ERROR");
	}

	@Test
	@DisplayName("그 외 예외는 지금처럼 500 이고 ERROR 로그를 남긴다")
	void otherExceptionIs500WithErrorLog(CapturedOutput output) throws Exception {
		mockMvc.perform(get("/test/bug"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."));

		// 위 404 테스트의 "ERROR 없음" 확인이 제대로 동작한다는 것을 보여주는 대조군이기도 합니다.
		assertThat(output).contains("처리하지 못한 예외");
	}

	@Test
	@DisplayName("업무 예외는 지금처럼 400 이고 메시지가 그대로 나간다")
	void businessExceptionUnchanged() throws Exception {
		mockMvc.perform(get("/test/business"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("이미 예약된 객실입니다."));
	}

	/** 테스트용 enum. 실제 도메인 enum 에 기대지 않으려고 따로 둡니다. */
	enum TestCategory { WIFI, PARKING }

	/** 테스트용 요청 본문. */
	record TestBody(TestCategory category) {
	}

	/** 예외를 일부러 일으키는 테스트 전용 컨트롤러. 실제 앱에는 등록되지 않습니다. */
	@RestController
	static class TestController {

		@GetMapping("/test/category")
		String category(@RequestParam TestCategory category) {
			return category.name();
		}

		@GetMapping("/test/items/{id}")
		String item(@PathVariable Long id) {
			return String.valueOf(id);
		}

		@PostMapping("/test/body")
		String body(@RequestBody TestBody body) {
			return body.category().name();
		}

		@GetMapping("/test/required")
		String required(@RequestParam String from) {
			return from;
		}

		@GetMapping("/test/no-resource")
		String noResource() throws NoResourceFoundException {
			throw new NoResourceFoundException(HttpMethod.GET, "favicon.ico");
		}

		@GetMapping("/test/bug")
		String bug() {
			throw new IllegalStateException("테스트용 버그");
		}

		@GetMapping("/test/business")
		String business() {
			throw new BusinessException("이미 예약된 객실입니다.");
		}
	}
}
