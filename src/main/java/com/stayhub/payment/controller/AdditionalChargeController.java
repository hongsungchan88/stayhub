package com.stayhub.payment.controller;

import com.stayhub.common.ApiResponse;
import com.stayhub.common.PageResponse;
import com.stayhub.payment.dto.AdditionalChargeCreateRequest;
import com.stayhub.payment.dto.AdditionalChargeResponse;
import com.stayhub.payment.dto.AdditionalChargeUpdateRequest;
import com.stayhub.payment.service.AdditionalChargeService;
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
 * 추가 요금 API.
 *
 * <p>HTTP 요청을 받아 서비스에 넘기고, 결과를 {@link ApiResponse} 로 감싸서 돌려줍니다.
 *
 * <pre>
 * GET    /api/additional-charges?page=0   목록 (page 는 생략 가능)
 * GET    /api/additional-charges/{id}     상세
 * POST   /api/additional-charges          등록
 * PUT    /api/additional-charges/{id}     수정
 * </pre>
 * 삭제 기능이 없는 테이블이라 DELETE API 는 없습니다.
 */
@RestController
@RequestMapping("/api/additional-charges")
public class AdditionalChargeController {

	private final AdditionalChargeService additionalChargeService;

	public AdditionalChargeController(AdditionalChargeService additionalChargeService) {
		this.additionalChargeService = additionalChargeService;
	}

	@GetMapping
	public ApiResponse<PageResponse<AdditionalChargeResponse>> getList(
			@RequestParam(defaultValue = "0") int page
	) {
		return ApiResponse.success(additionalChargeService.getList(page));
	}

	@GetMapping("/{id}")
	public ApiResponse<AdditionalChargeResponse> getDetail(@PathVariable Long id) {
		return ApiResponse.success(additionalChargeService.getDetail(id));
	}

	@PostMapping
	public ApiResponse<AdditionalChargeResponse> create(@Valid @RequestBody AdditionalChargeCreateRequest request) {
		AdditionalChargeResponse response = additionalChargeService.create(request);
		return ApiResponse.success(response);
	}

	@PutMapping("/{id}")
	public ApiResponse<AdditionalChargeResponse> update(
			@PathVariable Long id,
			@Valid @RequestBody AdditionalChargeUpdateRequest request
	) {
		return ApiResponse.success(additionalChargeService.update(id, request));
	}
}
