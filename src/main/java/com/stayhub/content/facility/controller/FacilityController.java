package com.stayhub.content.facility.controller;

import com.stayhub.common.ApiResponse;
import com.stayhub.common.PageResponse;
import com.stayhub.content.facility.dto.FacilityResponse;
import com.stayhub.content.facility.service.FacilityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 시설 코드 API. 조회만 있습니다.
 *
 * <pre>
 * GET /api/facilities?page=0     체크박스 목록 (use_yn = true 만, sort_order 순)
 * GET /api/facilities/{id}       상세 (id 는 시설 코드. 예: PARKING)
 * </pre>
 */
@RestController
@RequestMapping("/api/facilities")
public class FacilityController {

	private final FacilityService facilityService;

	public FacilityController(FacilityService facilityService) {
		this.facilityService = facilityService;
	}

	@GetMapping
	public ApiResponse<PageResponse<FacilityResponse>> getList(
			@RequestParam(defaultValue = "0") int page
	) {
		return ApiResponse.success(facilityService.getList(page));
	}

	@GetMapping("/{id}")
	public ApiResponse<FacilityResponse> getDetail(@PathVariable String id) {
		return ApiResponse.success(facilityService.getDetail(id));
	}
}
