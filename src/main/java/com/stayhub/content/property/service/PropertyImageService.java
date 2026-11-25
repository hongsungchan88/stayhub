package com.stayhub.content.property.service;

import com.stayhub.common.BusinessException;
import com.stayhub.common.PageResponse;
import com.stayhub.common.PropertyScope;
import com.stayhub.content.property.dto.PropertyImageCreateRequest;
import com.stayhub.content.property.dto.PropertyImageResponse;
import com.stayhub.content.property.dto.PropertyImageUpdateRequest;
import com.stayhub.content.property.entity.PropertyImage;
import com.stayhub.content.property.entity.PropertyStatus;
import com.stayhub.content.property.repository.PropertyImageRepository;
import com.stayhub.content.property.repository.PropertyRepository;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 숙소 이미지 업무 로직.
 *
 * <p>클래스에 {@code @Transactional(readOnly = true)}, 쓰기 메서드(등록·업로드·수정·삭제)에만 {@code @Transactional}.
 *
 * <p>유형 ③이라 status 조건이 없습니다.
 * {@code property_id} 는 등록할 때만 {@link PropertyScope} 값을 넣고, 조회 조건에는 넣지 않습니다(정책정의서 32행).
 *
 * <p><b>장수 규칙</b> (정의서 "대표 사진 1~10장", 총괄 결정 2026-11-25)
 * <ul>
 *   <li>최대 10장 — 등록·업로드할 때 막습니다</li>
 *   <li>최소 1장 — 숙소를 게시(PUBLISHED)로 바꿀 때 검사합니다({@code PropertyService.update}).
 *       그래서 게시 중에는 마지막 1장을 지울 수 없습니다</li>
 * </ul>
 * 숙소가 1번 하나뿐이라 장수는 테이블 전체 {@code count()} 로 셉니다.
 *
 * <p><b>업로드 검사</b> (CMS-001 "이미지 용량·확장자 검사", 총괄 결정)
 * <br>확장자(jpg·jpeg·png) <b>그리고</b> Content-Type(image/jpeg·image/png) 둘 다 맞아야 합니다. 10MB 는 톰캣이 먼저 막고
 * (GlobalExceptionHandler 400), 여기서도 한 번 더 확인합니다. jpeg 는 jpg 로 저장합니다(file_ext 는 jpg·png 두 값).
 */
@Service
@Transactional(readOnly = true)
public class PropertyImageService {

	/** 정의서 "이미지 업로드 최대 10장". */
	public static final int MAX_IMAGES = 10;

	/** application.yml spring.servlet.multipart.max-file-size 와 같게. */
	public static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

	public static final String TOO_MANY_IMAGES = "사진은 최대 " + MAX_IMAGES + "장까지 등록할 수 있습니다.";
	public static final String LAST_IMAGE_PUBLISHED = "게시 중에는 마지막 사진을 삭제할 수 없습니다. 게시 상태를 바꾸거나 다른 사진을 먼저 등록해 주세요.";
	public static final String FILE_REQUIRED = "업로드할 사진을 선택해 주세요.";
	public static final String NOT_ALLOWED_TYPE = "jpg, png 파일만 업로드할 수 있습니다.";
	/** GlobalExceptionHandler.handleMaxUploadSize 와 같은 문구. */
	public static final String TOO_LARGE = "파일은 10MB 이하만 올릴 수 있습니다.";

	/** 원래 확장자 → 저장할 확장자(file_ext). */
	private static final Map<String, String> ALLOWED_EXTENSIONS = Map.of("jpg", "jpg", "jpeg", "jpg", "png", "png");
	private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png");

	/** 목록 정렬: sort_order 오름차순 → 같으면 image_id 오름차순(등록 순). */
	private static final Sort LIST_SORT = Sort.by(
			Sort.Order.asc("sortOrder"),
			Sort.Order.asc("id")
	);

	private final PropertyImageRepository propertyImageRepository;
	private final PropertyRepository propertyRepository;
	private final PropertyImageFileStorage fileStorage;

	public PropertyImageService(
			PropertyImageRepository propertyImageRepository,
			PropertyRepository propertyRepository,
			PropertyImageFileStorage fileStorage
	) {
		this.propertyImageRepository = propertyImageRepository;
		this.propertyRepository = propertyRepository;
		this.fileStorage = fileStorage;
	}

	/**
	 * 목록. 한 페이지 20건.
	 *
	 * @param page 0부터 시작
	 */
	public PageResponse<PropertyImageResponse> getList(int page) {
		Pageable pageable = PageRequest.of(page, PageResponse.DEFAULT_PAGE_SIZE, LIST_SORT);
		return PageResponse.from(propertyImageRepository.findAll(pageable).map(PropertyImageResponse::from));
	}

	/** 상세. 없는 항목이면 BusinessException. */
	public PropertyImageResponse getDetail(Long id) {
		return PropertyImageResponse.from(findOrThrow(id));
	}

	/** 등록(경로 정보만). property_id 는 요청에서 받지 않고 여기서 상수로 넣습니다. 이미 10장이면 BusinessException. */
	@Transactional
	public PropertyImageResponse create(PropertyImageCreateRequest request) {
		requireRoomForOneMore();
		PropertyImage image = PropertyImage.create(
				PropertyScope.DEFAULT_PROPERTY_ID,
				request.filePath(),
				request.fileSize(),
				request.fileExt(),
				request.isMain(),
				request.sortOrder()
		);
		return PropertyImageResponse.from(propertyImageRepository.save(image));
	}

	/**
	 * 사진 파일 업로드. 검사 → UUID 이름으로 저장 → property_images 행 추가.
	 * 장수·확장자·Content-Type·크기 중 하나라도 맞지 않으면 파일을 저장하지 않고 BusinessException.
	 * 행 저장이 실패하면 방금 저장한 파일을 지웁니다.
	 */
	@Transactional
	public PropertyImageResponse upload(MultipartFile file) {
		requireRoomForOneMore();
		if (file == null || file.isEmpty()) {
			throw new BusinessException(FILE_REQUIRED);
		}
		if (file.getSize() > MAX_FILE_SIZE) {
			throw new BusinessException(TOO_LARGE);
		}
		String storedExtension = ALLOWED_EXTENSIONS.get(extensionOf(file.getOriginalFilename()));
		if (storedExtension == null || !ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
			throw new BusinessException(NOT_ALLOWED_TYPE);
		}

		String filePath;
		try (InputStream content = file.getInputStream()) {
			filePath = fileStorage.save(content, storedExtension);
		} catch (IOException e) {
			throw new UncheckedIOException("업로드한 사진을 읽을 수 없습니다.", e);
		}

		try {
			PropertyImage image = PropertyImage.create(
					PropertyScope.DEFAULT_PROPERTY_ID,
					filePath,
					(int) file.getSize(),
					storedExtension,
					null,
					null
			);
			return PropertyImageResponse.from(propertyImageRepository.save(image));
		} catch (RuntimeException e) {
			fileStorage.deleteQuietly(filePath); // 행이 없는 파일이 남지 않게
			throw e;
		}
	}

	/** 수정. 없는 항목이면 BusinessException. */
	@Transactional
	public PropertyImageResponse update(Long id, PropertyImageUpdateRequest request) {
		PropertyImage image = findOrThrow(id);
		image.update(
				request.filePath(),
				request.fileSize(),
				request.fileExt(),
				request.isMain(),
				request.sortOrder()
		);
		propertyImageRepository.flush();
		return PropertyImageResponse.from(image);
	}

	/**
	 * 삭제. 행을 실제로 지웁니다(정책정의서 24행 예외, 총괄 결정).
	 * 없는 항목이거나, 숙소가 게시 중인데 마지막 1장이면 BusinessException.
	 *
	 * @return 지운 사진의 file_path. 저장된 파일은 컨트롤러가 이 값으로 지웁니다(트랜잭션이 끝난 뒤라 파일 삭제가 실패해도 행 삭제는 그대로)
	 */
	@Transactional
	public String delete(Long id) {
		PropertyImage image = findOrThrow(id);
		if (isPublished() && propertyImageRepository.count() <= 1) {
			throw new BusinessException(LAST_IMAGE_PUBLISHED);
		}
		propertyImageRepository.delete(image);
		return image.getFilePath();
	}

	/** 10장이 이미 있으면 예외. 업로드는 파일을 저장하기 전에 먼저 확인합니다. */
	public void requireRoomForOneMore() {
		if (propertyImageRepository.count() >= MAX_IMAGES) {
			throw new BusinessException(TOO_MANY_IMAGES);
		}
	}

	/** 원래 파일 이름의 확장자(소문자). 확장자 판단에만 쓰고 이름 자체는 저장하지 않습니다. */
	private static String extensionOf(String originalFilename) {
		if (originalFilename == null || !originalFilename.contains(".")) {
			return "";
		}
		return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
	}

	/** 편집 대상 숙소(1번)가 게시 중인지. 행이 없으면 게시 중이 아닌 것으로 봅니다. */
	private boolean isPublished() {
		return propertyRepository.findById(PropertyScope.DEFAULT_PROPERTY_ID)
				.map(property -> property.getStatus() == PropertyStatus.PUBLISHED)
				.orElse(false);
	}

	/** 항목 하나를 찾습니다. 없으면 예외. */
	private PropertyImage findOrThrow(Long id) {
		return propertyImageRepository.findById(id)
				.orElseThrow(() -> new BusinessException("숙소 이미지를 찾을 수 없습니다."));
	}
}
