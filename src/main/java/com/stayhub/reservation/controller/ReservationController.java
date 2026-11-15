package com.stayhub.reservation.controller;

import com.stayhub.common.ApiResponse;
import com.stayhub.common.PageResponse;
import com.stayhub.reservation.dto.ReservationCreateRequest;
import com.stayhub.reservation.dto.ReservationResponse;
import com.stayhub.reservation.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import com.stayhub.reservation.dto.ReservationUpdateRequest;
import org.springframework.web.bind.annotation.PutMapping;

/**
 * 예약 목록·통합 검색·등록 API (RSV-001·006).
 *
 * <pre>
 * GET  /api/reservations?searchType=guestName&amp;keyword=&amp;page=0
 * POST /api/reservations
 * </pre>
 * 업무 로직과 DTO 변환은 서비스에서 처리합니다.
 * 예약 삭제 API는 없습니다.
 */
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping
    public ApiResponse<PageResponse<ReservationResponse>> getList(
            @RequestParam(defaultValue = "guestName") String searchType,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page
    ) {
        return ApiResponse.success(
                reservationService.getList(searchType, keyword, page));
    }

    @GetMapping("/{id}")
    public ApiResponse<ReservationResponse> getDetail(@PathVariable Long id) {
        return ApiResponse.success(reservationService.getDetail(id));
    }

    @PostMapping
    public ApiResponse<ReservationResponse> create(
            @Valid @RequestBody ReservationCreateRequest request
    ) {
        return ApiResponse.success(reservationService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ReservationResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ReservationUpdateRequest request
    ) {
        return ApiResponse.success(reservationService.update(id, request));
    }
}