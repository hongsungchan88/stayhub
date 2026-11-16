package com.stayhub.content.controller;

import com.stayhub.common.ApiResponse;
import com.stayhub.content.dto.PublicGuidebookResponse;
import com.stayhub.content.dto.PublicPropertyResponse;
import com.stayhub.content.entity.GuidebookCategory;
import com.stayhub.content.service.PublicContentService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 게스트(비로그인) 조회 전용 API. WebConfig 에서 /api/public/** 은 로그인 확인을 하지 않습니다.
 *
 * <pre>
 * GET /api/public/property                  숙소 소개 (PUBLISHED 가 아니면 400 "준비 중인 숙소입니다.")
 * GET /api/public/guidebooks?category=WIFI  가이드북 (category 는 생략 가능)
 * </pre>
 * GET 만 둡니다. 다른 메서드는 405 로 막힙니다. 등록·수정·삭제를 여기 추가하면 누구나 부를 수 있게 되니 절대 추가하지 마세요.
 *
 * <p>/api/public 아래 쓰기 API 는 게스트 예약 생성 POST /api/public/reservations 하나뿐입니다
 * ({@link com.stayhub.reservation.controller.PublicReservationController}, SCR-GST-03, 총괄 결정).
 * 그 밖의 쓰기 API 는 /api/public 에 두지 않습니다.
 */
@RestController
@RequestMapping("/api/public")
public class PublicContentController {

	private final PublicContentService publicContentService;

	public PublicContentController(PublicContentService publicContentService) {
		this.publicContentService = publicContentService;
	}

	@GetMapping("/property")
	public ApiResponse<PublicPropertyResponse> getProperty() {
		return ApiResponse.success(publicContentService.getProperty());
	}

	@GetMapping("/guidebooks")
	public ApiResponse<List<PublicGuidebookResponse>> getGuidebooks(
			@RequestParam(required = false) GuidebookCategory category
	) {
		return ApiResponse.success(publicContentService.getGuidebooks(category));
	}
}
