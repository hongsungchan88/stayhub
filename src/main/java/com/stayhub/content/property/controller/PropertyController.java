package com.stayhub.content.property.controller;

import com.stayhub.common.ApiResponse;
import com.stayhub.common.PageResponse;
import com.stayhub.content.property.dto.PropertyCreateRequest;
import com.stayhub.content.property.dto.PropertyResponse;
import com.stayhub.content.property.dto.PropertyUpdateRequest;
import com.stayhub.content.property.service.PropertyService;
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
 * 숙소 API.
 *
 * <p>HTTP 요청을 받아 서비스에 넘기고, 결과를 {@link ApiResponse} 로 감싸서 돌려줍니다.
 *
 * <pre>
 * GET    /api/properties?page=0     목록 (page 는 생략 가능)
 * GET    /api/properties/{id}       상세
 * POST   /api/properties            등록
 * PUT    /api/properties/{id}       수정 (게시 상태 변경 포함)
 * </pre>
 * 유형 ②라서 DELETE 가 없습니다.
 */
@RestController
@RequestMapping("/api/properties")
public class PropertyController {

	private final PropertyService propertyService;

	public PropertyController(PropertyService propertyService) {
		this.propertyService = propertyService;
	}

	@GetMapping
	public ApiResponse<PageResponse<PropertyResponse>> getList(
			@RequestParam(defaultValue = "0") int page
	) {
		return ApiResponse.success(propertyService.getList(page));
	}

	@GetMapping("/{id}")
	public ApiResponse<PropertyResponse> getDetail(@PathVariable Long id) {
		return ApiResponse.success(propertyService.getDetail(id));
	}

	@PostMapping
	public ApiResponse<PropertyResponse> create(@Valid @RequestBody PropertyCreateRequest request) {
		PropertyResponse response = propertyService.create(request);
		return ApiResponse.success(response);
	}

	@PutMapping("/{id}")
	public ApiResponse<PropertyResponse> update(
			@PathVariable Long id,
			@Valid @RequestBody PropertyUpdateRequest request
	) {
		return ApiResponse.success(propertyService.update(id, request));
	}
}
