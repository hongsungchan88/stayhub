package com.stayhub.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 공통 응답 형식이 의도대로 만들어지는지 확인합니다.
 *
 * <p>DB 나 서버를 띄우지 않는 순수 단위 테스트라 어디서든 바로 돌아갑니다.
 * 테스트를 처음 써보는 팀원은 이 파일을 본보기로 삼으세요.
 */
class ApiResponseTest {

	@Test
	@DisplayName("성공 응답은 success 가 true 이고 데이터를 담는다")
	void success() {
		ApiResponse<String> response = ApiResponse.success("예약 목록");

		assertThat(response.isSuccess()).isTrue();
		assertThat(response.getData()).isEqualTo("예약 목록");
		assertThat(response.getMessage()).isNull();
	}

	@Test
	@DisplayName("메시지를 함께 담은 성공 응답을 만들 수 있다")
	void successWithMessage() {
		ApiResponse<String> response = ApiResponse.success("예약 목록", "조회되었습니다.");

		assertThat(response.isSuccess()).isTrue();
		assertThat(response.getData()).isEqualTo("예약 목록");
		assertThat(response.getMessage()).isEqualTo("조회되었습니다.");
	}

	@Test
	@DisplayName("실패 응답은 success 가 false 이고 데이터가 비어 있다")
	void error() {
		ApiResponse<String> response = ApiResponse.error("예약을 찾을 수 없습니다.");

		assertThat(response.isSuccess()).isFalse();
		assertThat(response.getData()).isNull();
		assertThat(response.getMessage()).isEqualTo("예약을 찾을 수 없습니다.");
	}
}
