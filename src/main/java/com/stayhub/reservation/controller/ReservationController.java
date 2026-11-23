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
import com.stayhub.reservation.dto.ReservationCancelRequest;
import com.stayhub.account.dto.LoginUser;
import com.stayhub.account.entity.UserRole;
import com.stayhub.common.ForbiddenException;
import com.stayhub.common.UnauthorizedException;
import org.springframework.web.bind.annotation.SessionAttribute;

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

    /** 예약 취소 및 결제 완료 건 환불. */
    @PostMapping("/{id}/cancel")
    public ApiResponse<ReservationResponse> cancel(
            @PathVariable Long id,
            @Valid @RequestBody ReservationCancelRequest request,
            @SessionAttribute(
                    name = LoginUser.SESSION_KEY,
                    required = false
            ) LoginUser loginUser
    ) {
        requireReservationStaff(loginUser);

        return ApiResponse.success(
                reservationService.cancel(id, request.cancelReason()));
    }

    /** 선택한 예약의 수동 노쇼 처리. */
    @PostMapping("/{id}/no-show")
    public ApiResponse<ReservationResponse> markNoShow(
            @PathVariable Long id,
            @SessionAttribute(
                    name = LoginUser.SESSION_KEY,
                    required = false
            ) LoginUser loginUser
    ) {
        requireReservationStaff(loginUser);

        return ApiResponse.success(
                reservationService.markNoShow(id));
    }

    /** 프런트가 수동으로 처리할 노쇼 대상 목록. */
    @GetMapping("/no-show-candidates")
    public ApiResponse<PageResponse<ReservationResponse>> getNoShowCandidates(
            @RequestParam(defaultValue = "0") int page,
            @SessionAttribute(
                    name = LoginUser.SESSION_KEY,
                    required = false
            ) LoginUser loginUser
    ) {
        requireReservationStaff(loginUser);

        return ApiResponse.success(
                reservationService.getNoShowCandidates(page));
    }

    /** 호스트와 프런트 직원만 허용합니다. */
    private void requireReservationStaff(LoginUser loginUser) {
        if (loginUser == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }

        if (loginUser.role() != UserRole.HOST
                && loginUser.role() != UserRole.FRONT) {
            throw new ForbiddenException("예약 관리 권한이 없습니다.");
        }
    }
}