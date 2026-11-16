package com.stayhub.reservation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 예약 관리자 화면 (SCR-RSV-01 목록, SCR-RSV-03 등록).
 *
 * <p>공통 관리자 레이아웃을 사용하는 화면 틀입니다.
 * 조회·검색·저장은 화면에서 API를 호출해 처리합니다.
 * Model에 예약 데이터를 넣지 않습니다.
 *
 * <pre>
 * GET /admin/reservations     → templates/admin/reservations/list.html
 * GET /admin/reservations/new → templates/admin/reservations/form.html
 * </pre>
 */
@Controller
@RequestMapping("/admin/reservations")
public class ReservationPageController {

    @GetMapping
    public String list() {
        return "admin/reservations/list";
    }

    @GetMapping("/{id}")
    public String detail() {
        return "admin/reservations/detail";
    }

    @GetMapping("/new")
    public String create() {
        return "admin/reservations/form";
    }

    @GetMapping("/{id}/edit")
    public String edit() {
        return "admin/reservations/form";
    }
}