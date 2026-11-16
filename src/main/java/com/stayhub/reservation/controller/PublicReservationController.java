package com.stayhub.reservation.controller;

import com.stayhub.common.ApiResponse;
import com.stayhub.reservation.dto.PublicAvailabilityResponse;
import com.stayhub.reservation.dto.PublicReservationRequest;
import com.stayhub.reservation.dto.PublicReservationResponse;
import com.stayhub.reservation.service.PublicReservationService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 게스트(비로그인) 예약 API. /api/public/** 라 로그인 확인을 하지 않습니다(WebConfig).
 *
 * <pre>
 * GET  /api/public/rooms/available?checkIn=2026-12-01&checkOut=2026-12-03&guests=2   빈 객실 + 대체 날짜 (CMS-008)
 * POST /api/public/reservations                                                    예약 생성 (SCR-GST-03)
 * </pre>
 * POST 는 /api/public 아래 유일한 쓰기 API 입니다(총괄 결정). 다른 쓰기 API 를 여기 추가하지 마세요.
 */
@RestController
@RequestMapping("/api/public")
public class PublicReservationController {

	private final PublicReservationService publicReservationService;

	public PublicReservationController(PublicReservationService publicReservationService) {
		this.publicReservationService = publicReservationService;
	}

	@GetMapping("/rooms/available")
	public ApiResponse<PublicAvailabilityResponse> getAvailability(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
			@RequestParam Integer guests
	) {
		return ApiResponse.success(publicReservationService.getAvailability(checkIn, checkOut, guests));
	}

	@PostMapping("/reservations")
	public ApiResponse<PublicReservationResponse> create(@Valid @RequestBody PublicReservationRequest request) {
		return ApiResponse.success(publicReservationService.create(request));
	}
}
