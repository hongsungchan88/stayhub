package com.stayhub.reservation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** 비로그인 게스트에게 내 예약 조회 화면을 엽니다. */
@Controller
public class GuestReservationLookupPageController {

    /** 예약번호와 연락처를 입력하는 화면을 반환합니다. */
    @GetMapping("/my-reservation")
    public String myReservation() {
        return "guest/my-reservation";
    }
}
