package com.stayhub.reservation.controller;

import com.stayhub.account.dto.LoginUser;
import com.stayhub.account.entity.UserRole;
import com.stayhub.common.*;
import com.stayhub.reservation.dto.CheckInRequest;
import com.stayhub.reservation.service.CheckInOutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class CheckInOutController {
    private final CheckInOutService service;

    private boolean allowed(LoginUser u) {
        return u != null && (u.role() == UserRole.HOST || u.role() == UserRole.FRONT);
    }

    private Long staffId(LoginUser u) {
        if (u == null) throw new UnauthorizedException("로그인이 필요합니다.");
        if (!allowed(u)) throw new ForbiddenException("예약 관리 권한이 없습니다.");
        return u.userId();
    }

    @GetMapping("/admin/check-in-out")
    public String desk(@SessionAttribute(name = LoginUser.SESSION_KEY, required = false) LoginUser u) {
        if (u == null) return "redirect:/login";
        return allowed(u) ? "admin/reservations/check-in-out" : "redirect:/403";
    }

    @ResponseBody @GetMapping("/api/reservations/check-in-out")
    public ApiResponse<PageResponse<CheckInOutService.DeskRow>> rows(
            @RequestParam(defaultValue = "0") int page,
            @SessionAttribute(name = LoginUser.SESSION_KEY, required = false) LoginUser u) {
        staffId(u);
        return ApiResponse.success(service.list(page));
    }

    @ResponseBody @PostMapping("/api/reservations/{id}/check-in")
    public ApiResponse<Void> checkIn(@PathVariable Long id,
                                     @Valid @RequestBody CheckInRequest request,
                                     @SessionAttribute(name = LoginUser.SESSION_KEY, required = false) LoginUser u) {
        service.checkIn(id, request.keyCode(), staffId(u));
        return ApiResponse.success(null);
    }

    @ResponseBody @PostMapping("/api/reservations/{id}/check-out")
    public ApiResponse<Void> checkOut(@PathVariable Long id,
                                      @SessionAttribute(name = LoginUser.SESSION_KEY, required = false) LoginUser u) {
        service.checkOut(id, staffId(u));
        return ApiResponse.success(null);
    }
}