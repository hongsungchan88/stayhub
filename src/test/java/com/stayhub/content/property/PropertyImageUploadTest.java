package com.stayhub.content.property;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stayhub.account.dto.LoginUser;
import com.stayhub.account.entity.UserRole;
import com.stayhub.common.BusinessException;
import com.stayhub.common.PropertyScope;
import com.stayhub.content.property.dto.PropertyImageResponse;
import com.stayhub.content.property.entity.PropertyImage;
import com.stayhub.content.property.repository.PropertyImageRepository;
import com.stayhub.content.property.service.PropertyImageFileStorage;
import com.stayhub.content.property.service.PropertyImageService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 숙소 사진 업로드·파일 삭제 테스트 (SCR-CMS-01, 총괄 결정 2026-11-25).
 *
 * <p>업로드 폴더는 UploadConfigTest 처럼 임시 폴더로 바꿔 실제 폴더를 건드리지 않습니다.
 * DB 행은 {@code @Transactional} 로 롤백됩니다(임시 폴더의 파일은 테스트가 끝나면 JUnit 이 지웁니다).
 * 10MB 초과는 톰캣이 먼저 막으므로 UploadConfigTest(총괄)가 확인합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PropertyImageUploadTest {

	@TempDir
	static Path tempDir;

	@DynamicPropertySource
	static void uploadDir(DynamicPropertyRegistry registry) {
		registry.add("stayhub.upload-dir", () -> tempDir.resolve("uploads").toString());
	}

	@Autowired
	private PropertyImageService propertyImageService;

	@Autowired
	private PropertyImageFileStorage fileStorage;

	@Autowired
	private PropertyImageRepository propertyImageRepository;

	@Autowired
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		// 경고: @Transactional을 지우면 개발 DB의 이 테이블이 실제로 비워집니다. 지우지 마세요.
		propertyImageRepository.deleteAll();
	}

	@Test
	@DisplayName("jpg 를 올리면 UUID 이름으로 저장되고, 원래 파일 이름은 경로에 들어가지 않는다")
	void jpgIsStoredWithUuidName() throws IOException {
		PropertyImageResponse saved = propertyImageService.upload(
				new MockMultipartFile("file", "../../바다 사진.jpg", "image/jpeg", new byte[] {1, 2, 3}));

		assertThat(saved.filePath()).matches("^/uploads/[0-9a-f-]{36}\\.jpg$");
		assertThat(saved.fileExt()).isEqualTo("jpg");
		assertThat(saved.fileSize()).isEqualTo(3);
		assertThat(Files.readAllBytes(storedFile(saved.filePath()))).containsExactly(1, 2, 3);
		assertThat(propertyImageRepository.findById(saved.id()))
				.get()
				.extracting(PropertyImage::getPropertyId)
				.isEqualTo(PropertyScope.DEFAULT_PROPERTY_ID);
	}

	@Test
	@DisplayName("jpeg 는 jpg 로, png 는 png 로 저장한다")
	void jpegIsStoredAsJpg() {
		assertThat(propertyImageService.upload(image("a.JPEG", "image/jpeg")).fileExt()).isEqualTo("jpg");
		assertThat(propertyImageService.upload(image("b.png", "image/png")).fileExt()).isEqualTo("png");
	}

	@Test
	@DisplayName("확장자나 Content-Type 이 하나라도 맞지 않으면 거부하고 파일을 남기지 않는다")
	void wrongTypeIsRejected() throws IOException {
		long before = fileCount();

		assertThatThrownBy(() -> propertyImageService.upload(image("a.gif", "image/gif")))
				.isInstanceOf(BusinessException.class)
				.hasMessage(PropertyImageService.NOT_ALLOWED_TYPE);
		// 확장자는 png 인데 내용 형식이 다름
		assertThatThrownBy(() -> propertyImageService.upload(image("a.png", "text/plain")))
				.isInstanceOf(BusinessException.class)
				.hasMessage(PropertyImageService.NOT_ALLOWED_TYPE);
		// 내용 형식은 jpeg 인데 확장자가 다름
		assertThatThrownBy(() -> propertyImageService.upload(image("a.exe", "image/jpeg")))
				.isInstanceOf(BusinessException.class)
				.hasMessage(PropertyImageService.NOT_ALLOWED_TYPE);
		assertThatThrownBy(() -> propertyImageService.upload(null))
				.isInstanceOf(BusinessException.class)
				.hasMessage(PropertyImageService.FILE_REQUIRED);

		assertThat(fileCount()).isEqualTo(before);
		assertThat(propertyImageRepository.count()).isZero();
	}

	@Test
	@DisplayName("10장이 있으면 업로드를 거부하고 파일을 저장하지 않는다")
	void eleventhUploadIsRejected() throws IOException {
		for (int i = 0; i < PropertyImageService.MAX_IMAGES; i++) {
			propertyImageService.upload(image(i + ".jpg", "image/jpeg"));
		}
		long before = fileCount();

		assertThatThrownBy(() -> propertyImageService.upload(image("11.jpg", "image/jpeg")))
				.isInstanceOf(BusinessException.class)
				.hasMessage(PropertyImageService.TOO_MANY_IMAGES);
		assertThat(fileCount()).isEqualTo(before);
	}

	@Test
	@DisplayName("파일 삭제: 업로드 폴더의 파일은 지우고, 폴더 밖·예전 경로는 건너뛴다(예외 없음)")
	void deleteQuietlyRemovesOnlyUploadedFile() throws IOException {
		PropertyImageResponse saved = propertyImageService.upload(image("a.jpg", "image/jpeg"));
		Path file = storedFile(saved.filePath());
		Path outside = Files.writeString(tempDir.resolve("outside.txt"), "keep");

		fileStorage.deleteQuietly(saved.filePath());
		fileStorage.deleteQuietly("/uploads/../outside.txt"); // 폴더 밖 → 지우지 않음
		fileStorage.deleteQuietly("/upload/property/1.jpg"); // 예전 테스트 데이터 경로 → 건너뜀
		fileStorage.deleteQuietly("/uploads/none.jpg");      // 이미 없는 파일 → 그냥 지나감

		assertThat(file).doesNotExist();
		assertThat(outside).exists();
	}

	@Test
	@DisplayName("API: HOST 로그인 후 POST /api/property-images/upload (multipart file) → success, /uploads/ 경로")
	void uploadApi() throws Exception {
		MockHttpSession host = new MockHttpSession();
		host.setAttribute(LoginUser.SESSION_KEY, new LoginUser(1L, "tester", "테스트사용자", UserRole.HOST));

		mockMvc.perform(multipart("/api/property-images/upload").file(image("a.png", "image/png")).session(host))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.filePath").value(org.hamcrest.Matchers.startsWith("/uploads/")));

		mockMvc.perform(multipart("/api/property-images/upload").session(host)) // 파일 없음
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(PropertyImageService.FILE_REQUIRED));
	}

	private MockMultipartFile image(String name, String contentType) {
		return new MockMultipartFile("file", name, contentType, new byte[] {1});
	}

	private Path storedFile(String filePath) {
		return tempDir.resolve("uploads").resolve(filePath.substring(PropertyImageFileStorage.URL_PREFIX.length()));
	}

	private long fileCount() throws IOException {
		Path dir = tempDir.resolve("uploads");
		if (!Files.exists(dir)) {
			return 0;
		}
		try (Stream<Path> files = Files.list(dir)) {
			return files.count();
		}
	}
}
