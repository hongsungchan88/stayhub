package com.stayhub.content.property.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 숙소 사진 파일을 업로드 폴더에 저장·삭제합니다 (SCR-CMS-01).
 *
 * <p><b>이 파일이 하는 일</b>
 * <ul>
 *   <li>저장 — 이름은 <b>UUID + 확장자</b>만 씁니다. 사용자가 올린 원래 파일 이름은 경로에 절대 넣지 않습니다(총괄 결정).
 *       {@code ../} 같은 이름으로 다른 폴더에 쓰는 공격을 막고, 한글·공백 이름 문제도 없앱니다</li>
 *   <li>DB 에 넣을 주소는 {@code /uploads/파일명} 입니다. 공통 설정(WebConfig)이 이 주소로 파일을 보여 줍니다</li>
 *   <li>삭제 — 실패해도 예외를 던지지 않고 로그만 남깁니다(행 삭제는 이미 끝난 뒤라서, 총괄 결정)</li>
 * </ul>
 * 폴더는 {@code stayhub.upload-dir}(application.yml, 기본 ${user.home}/stayhub-uploads)이고, 앱 시작 때 WebConfig 가 만듭니다.
 */
@Component
public class PropertyImageFileStorage {

	private static final Logger log = LoggerFactory.getLogger(PropertyImageFileStorage.class);

	/** DB file_path 와 화면 주소의 앞부분. WebConfig 의 addResourceHandler("/uploads/**") 와 같게. */
	public static final String URL_PREFIX = "/uploads/";

	private final Path uploadDir;

	public PropertyImageFileStorage(@Value("${stayhub.upload-dir}") String uploadDir) {
		this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
	}

	/**
	 * 파일을 UUID 이름으로 저장하고 DB 에 넣을 주소를 돌려줍니다.
	 *
	 * @param extension 검사를 마친 확장자(jpg·png). 원래 파일 이름에서 가져온 값이지만 여기 오기 전에 정해진 값만 옵니다
	 * @return 예) {@code /uploads/3f2b...e1.jpg}
	 */
	public String save(InputStream content, String extension) {
		String fileName = UUID.randomUUID() + "." + extension;
		try {
			Files.createDirectories(uploadDir);
			Files.copy(content, uploadDir.resolve(fileName));
		} catch (IOException e) {
			throw new UncheckedIOException("사진 파일을 저장할 수 없습니다: " + fileName, e);
		}
		return URL_PREFIX + fileName;
	}

	/**
	 * {@code /uploads/파일명} 의 파일을 지웁니다. 실패하거나 업로드 폴더 밖을 가리키면 로그만 남깁니다.
	 * {@code /uploads/} 로 시작하지 않는 예전 테스트 데이터(파일 없이 경로만 넣은 행)는 지울 파일이 없으니 건너뜁니다.
	 */
	public void deleteQuietly(String filePath) {
		if (filePath == null || !filePath.startsWith(URL_PREFIX)) {
			return;
		}
		Path target = uploadDir.resolve(filePath.substring(URL_PREFIX.length())).normalize();
		if (!target.getParent().equals(uploadDir)) { // 업로드 폴더 바로 아래 파일만
			log.warn("업로드 폴더 밖 경로라 지우지 않음: {}", filePath);
			return;
		}
		try {
			Files.deleteIfExists(target);
		} catch (IOException e) {
			log.warn("사진 파일 삭제 실패(행은 이미 삭제됨): {}", target, e);
		}
	}
}
