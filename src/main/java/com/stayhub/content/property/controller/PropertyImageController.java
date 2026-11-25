package com.stayhub.content.property.controller;

import com.stayhub.common.ApiResponse;
import com.stayhub.common.PageResponse;
import com.stayhub.content.property.dto.PropertyImageCreateRequest;
import com.stayhub.content.property.dto.PropertyImageResponse;
import com.stayhub.content.property.dto.PropertyImageUpdateRequest;
import com.stayhub.content.property.service.PropertyImageFileStorage;
import com.stayhub.content.property.service.PropertyImageService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 숙소 이미지 API.
 *
 * <p>팀 URL 규칙에 따라 하이픈을 씁니다({@code /api/property-images}). 테이블명은 {@code property_images} 그대로입니다.
 *
 * <pre>
 * GET    /api/property-images?page=0     목록 (page 는 생략 가능)
 * GET    /api/property-images/{id}       상세
 * POST   /api/property-images            등록 (경로 정보만, 최대 10장)
 * POST   /api/property-images/upload     사진 파일 업로드 (multipart, 칸 이름 file) — SCR-CMS-01 화면이 씁니다
 * PUT    /api/property-images/{id}       수정
 * DELETE /api/property-images/{id}       삭제 (행 + 파일, 게시 중 마지막 1장은 불가 — 총괄 결정 2026-11-25)
 * </pre>
 */
@RestController
@RequestMapping("/api/property-images")
public class PropertyImageController {

	private final PropertyImageService propertyImageService;
	private final PropertyImageFileStorage fileStorage;

	public PropertyImageController(PropertyImageService propertyImageService, PropertyImageFileStorage fileStorage) {
		this.propertyImageService = propertyImageService;
		this.fileStorage = fileStorage;
	}

	@GetMapping
	public ApiResponse<PageResponse<PropertyImageResponse>> getList(
			@RequestParam(defaultValue = "0") int page
	) {
		return ApiResponse.success(propertyImageService.getList(page));
	}

	@GetMapping("/{id}")
	public ApiResponse<PropertyImageResponse> getDetail(@PathVariable Long id) {
		return ApiResponse.success(propertyImageService.getDetail(id));
	}

	@PostMapping
	public ApiResponse<PropertyImageResponse> create(@Valid @RequestBody PropertyImageCreateRequest request) {
		return ApiResponse.success(propertyImageService.create(request));
	}

	/** 파일이 빠져도 500 이 아니라 "업로드할 사진을 선택해 주세요." 가 나가도록 required = false 로 받고 서비스에서 확인합니다. */
	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<PropertyImageResponse> upload(@RequestParam(value = "file", required = false) MultipartFile file) {
		return ApiResponse.success(propertyImageService.upload(file));
	}

	@PutMapping("/{id}")
	public ApiResponse<PropertyImageResponse> update(
			@PathVariable Long id,
			@Valid @RequestBody PropertyImageUpdateRequest request
	) {
		return ApiResponse.success(propertyImageService.update(id, request));
	}

	@DeleteMapping("/{id}")
	public ApiResponse<Void> delete(@PathVariable Long id) {
		String filePath = propertyImageService.delete(id);
		// 행 삭제(트랜잭션)가 끝난 뒤 파일을 지웁니다. 실패해도 로그만 남기고 성공으로 응답합니다(총괄 결정).
		fileStorage.deleteQuietly(filePath);
		return ApiResponse.success(null);
	}
}
