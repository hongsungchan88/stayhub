package com.stayhub.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * 사진 업로드 공통 설정 테스트 (SCR-CMS-01).
 *
 * <p>크기 제한은 톰캣이 확인하므로 가짜 요청(MockMvc)으로는 확인되지 않습니다. 그래서 실제 서버를 띄웁니다.
 * 업로드 폴더는 임시 폴더로 바꿔 실제 폴더를 건드리지 않습니다.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class UploadConfigTest {

	private static final int MB = 1024 * 1024;

	@TempDir
	static Path tempDir;

	@DynamicPropertySource
	static void uploadDir(DynamicPropertyRegistry registry) {
		// 아직 없는 하위 폴더 — 앱이 시작될 때 만들어져야 합니다.
		registry.add("stayhub.upload-dir", () -> tempDir.resolve("uploads").toString());
	}

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	@DisplayName("시작할 때 폴더가 만들어지고, /uploads/파일명 은 로그인 없이 200")
	void uploadedFileIsOpenWithoutLogin() throws Exception {
		Files.writeString(tempDir.resolve("uploads").resolve("test.txt"), "photo");

		ResponseEntity<String> response = restTemplate.getForEntity("/uploads/test.txt", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo("photo");
	}

	@Test
	@DisplayName("/uploads/ 에 없는 파일은 404")
	void missingFileIs404() {
		assertThat(restTemplate.getForEntity("/uploads/none.jpg", String.class).getStatusCode())
				.isEqualTo(HttpStatus.NOT_FOUND);
	}

	@ParameterizedTest(name = "{0}바이트 업로드는 400 과 안내 문구")
	@ValueSource(ints = {10 * MB + 1, 11 * MB, 20 * MB})
	void tooLargeUploadIs400(int bytes) {
		// 크기 확인은 컨트롤러를 찾기 전에 하므로 아무 주소로나 보내도 됩니다.
		ResponseEntity<String> response = upload(bytes);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).contains("\"success\":false", "파일은 10MB 이하만 올릴 수 있습니다.");
	}

	@Test
	@DisplayName("정확히 10MB 인 파일은 크기 제한에 걸리지 않는다")
	void exactly10MbPassesSizeCheck() {
		assertThat(upload(10 * MB).getBody()).doesNotContain("10MB");
	}

	private ResponseEntity<String> upload(int bytes) {
		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add("file", new ByteArrayResource(new byte[bytes]) {
			@Override
			public String getFilename() {
				return "big.jpg";
			}
		});
		return restTemplate.postForEntity("/api/auth/login", body, String.class);
	}
}
