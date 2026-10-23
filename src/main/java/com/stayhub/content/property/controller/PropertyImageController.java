package com.stayhub.content.property.controller;

import com.stayhub.common.ApiResponse;
import com.stayhub.common.PageResponse;
import com.stayhub.content.property.dto.PropertyImageCreateRequest;
import com.stayhub.content.property.dto.PropertyImageResponse;
import com.stayhub.content.property.dto.PropertyImageUpdateRequest;
import com.stayhub.content.property.service.PropertyImageService;
import jakarta.validation.Valid;
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
 * POST   /api/property-images            등록
 * PUT    /api/property-images/{id}       수정
 * </pre>
 * 삭제 기능은 없습니다(유형 ③).
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
}
