package com.stayhub.reservation.controller;

import com.stayhub.common.ApiResponse;
import com.stayhub.common.PageResponse;
import com.stayhub.reservation.dto.SpecialRequestCreateRequest;
import com.stayhub.reservation.dto.SpecialRequestResponse;
import com.stayhub.reservation.dto.SpecialRequestUpdateRequest;
import com.stayhub.reservation.service.SpecialRequestService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 특별 요청사항 등록·조회·처리 정보 변경 API. */
@RestController
@RequestMapping("/api/special-requests")
public class SpecialRequestController {

    private final SpecialRequestService specialRequestService;

    public SpecialRequestController(
            SpecialRequestService specialRequestService
    ) {
        this.specialRequestService = specialRequestService;
    }

    @GetMapping
    public ApiResponse<PageResponse<SpecialRequestResponse>> getList(
            @RequestParam(required = false) Long reservationId,
            @RequestParam(defaultValue = "0") int page
    ) {
        return ApiResponse.success(
                specialRequestService.getList(reservationId, page));
    }

    @GetMapping("/{id}")
    public ApiResponse<SpecialRequestResponse> getDetail(
            @PathVariable Long id
    ) {
        return ApiResponse.success(
                specialRequestService.getDetail(id));
    }

    @PostMapping
    public ApiResponse<SpecialRequestResponse> create(
            @Valid @RequestBody SpecialRequestCreateRequest request
    ) {
        return ApiResponse.success(
                specialRequestService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<SpecialRequestResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SpecialRequestUpdateRequest request
    ) {
        return ApiResponse.success(
                specialRequestService.update(id, request));
    }
}