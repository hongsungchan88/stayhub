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
 * <p>HTTP 요청을 받아 서비스에 넘기고, 결과를 {@link ApiResponse} 로 감싸서 돌려줍니다.
 *
 * <pre>
 * GET    /api/property_images?page=0     목록 (page 는 생략 가능)
 * GET    /api/property_images/{id}       상세
 * POST   /api/property_images            등록
 * PUT    /api/property_images/{id}       수정
 * </pre>
 * 유형 ③이라 DELETE 가 없습니다.
 */
@RestController
@RequestMapping("/api/property_images")
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
		PropertyImageResponse response = propertyImageService.create(request);
		return ApiResponse.success(response);
	}

	@PutMapping("/{id}")
	public ApiResponse<PropertyImageResponse> update(
			@PathVariable Long id,
			@Valid @RequestBody PropertyImageUpdateRequest request
	) {
		return ApiResponse.success(propertyImageService.update(id, request));
	}
}
