package com.stayhub.reservation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** 특별 요청사항 관리자 화면. */
@Controller
@RequestMapping("/admin/special-requests")
public class SpecialRequestPageController {

    @GetMapping
    public String list() {
        return "admin/special-requests/list";
    }
}