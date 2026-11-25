package com.stayhub.content.property.controller;

import com.stayhub.common.ApiResponse;
import com.stayhub.common.PageResponse;
import com.stayhub.content.property.dto.PropertyImageCreateRequest;
import com.stayhub.content.property.dto.PropertyImageResponse;
import com.stayhub.content.property.dto.PropertyImageUpdateRequest;
import com.stayhub.content.property.service.PropertyImageService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 숙소 이미지 API.
 *
 * <p>팀 URL 규칙에 따라 하이픈을 씁니다({@code /api/property-images}). 테이블명은 {@code property_images} 그대로입니다.
 *
 * <pre>
 * GET    /api/property-images?page=0     목록 (page 는 생략 가능)
 * GET    /api/property-images/{id}       상세
 * POST   /api/property-images            등록 (최대 10장)
 * PUT    /api/property-images/{id}       수정
 * DELETE /api/property-images/{id}       삭제 (행을 실제로 지움, 게시 중 마지막 1장은 불가 — 총괄 결정 2026-11-25)
 * </pre>
 */
@RestController
@RequestMapping("/api/property-images")
public class PropertyImageController {

	private final PropertyImageService propertyImageService;

	public PropertyImageController(PropertyImageService propertyImageService) {
		this.propertyImageService = propertyImageService;
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

	@PutMapping("/{id}")
	public ApiResponse<PropertyImageResponse> update(
			@PathVariable Long id,
			@Valid @RequestBody PropertyImageUpdateRequest request
	) {
		return ApiResponse.success(propertyImageService.update(id, request));
	}

	@DeleteMapping("/{id}")
	public ApiResponse<Void> delete(@PathVariable Long id) {
		propertyImageService.delete(id);
		// 저장된 파일 삭제는 업로드 API 와 함께 추가합니다(공통 설정 PR 머지 후).
		return ApiResponse.success(null);
	}
}
