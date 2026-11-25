package com.stayhub.reservation.controller;

import com.stayhub.account.dto.LoginUser;
import com.stayhub.common.ApiResponse;
import com.stayhub.common.BusinessException;
import com.stayhub.reservation.dto.GuestReservationLookupRequest;
import com.stayhub.reservation.dto.GuestReservationLookupResponse;
import com.stayhub.reservation.service.GuestReservationLookupService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 게스트 예약 본인 확인과 임시 조회 세션을 처리합니다. 예약 상태는 바꾸지 않습니다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/reservations/lookup")
public class GuestReservationLookupController {

    private static final String GRANT_KEY = GuestReservationLookupController.class.getName() + ".grant";
    private static final Duration LOOKUP_DURATION = Duration.ofMinutes(30);

    private final GuestReservationLookupService lookupService;

    /** 입력은 요청 본문으로만 받고, 성공한 경우 해당 예약을 30분 동안 열 수 있게 합니다. */
    @PostMapping
    public ApiResponse<GuestReservationLookupResponse> verify(
            @Valid @RequestBody GuestReservationLookupRequest lookupRequest,
            HttpServletRequest request, HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        HttpSession existing = request.getSession(false);
        if (existing != null) {
            existing.removeAttribute(GRANT_KEY);
        }

        Long reservationId = lookupService.verify(lookupRequest);
        GuestReservationLookupResponse detail = lookupService.findDetail(reservationId)
                .orElseThrow(() -> new BusinessException("예약번호와 연락처를 다시 확인해 주세요."));

        HttpSession session = request.getSession();
        if (existing != null && session.getAttribute(LoginUser.SESSION_KEY) == null) {
            request.changeSessionId();
        }
        session.setAttribute(GRANT_KEY, new LookupGrant(reservationId, Instant.now().plus(LOOKUP_DURATION)));
        return ApiResponse.success(detail);
    }

    /** 확인한 예약이 유효하면 다시 보여주고, 없거나 만료되었으면 빈 결과를 반환합니다. */
    @GetMapping
    public ApiResponse<GuestReservationLookupResponse> current(
            HttpServletRequest request, HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute(GRANT_KEY) instanceof LookupGrant grant)) {
            return ApiResponse.success(null);
        }
        if (!Instant.now().isBefore(grant.expiresAt())) {
            session.removeAttribute(GRANT_KEY);
            return ApiResponse.success(null);
        }
        return ApiResponse.success(lookupService.findDetail(grant.reservationId()).orElse(null));
    }

    private record LookupGrant(Long reservationId, Instant expiresAt) {
    }
}
