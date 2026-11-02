package com.stayhub.content.property.controller;

import com.stayhub.common.ApiResponse;
import com.stayhub.content.property.service.PropertyFacilityService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 숙소-시설 매핑 API. 조회만 있습니다.
 *
 * <pre>
 * GET /api/property-facilities     숙소에 체크된 시설 코드 목록 (예: ["PARKING","WIFI"])
 * </pre>
 * 저장은 숙소 소개 저장(PUT /api/properties/{id}) 의 facilityCodes 로 함께 합니다.
 */
@RestController
@RequestMapping("/api/property-facilities")
public class PropertyFacilityController {

	private final PropertyFacilityService propertyFacilityService;

	public PropertyFacilityController(PropertyFacilityService propertyFacilityService) {
		this.propertyFacilityService = propertyFacilityService;
	}

	@GetMapping
	public ApiResponse<List<String>> getCodes() {
		return ApiResponse.success(propertyFacilityService.getCodes());
	}
}
