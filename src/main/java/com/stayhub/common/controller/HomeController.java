package com.stayhub.common.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 게스트(비로그인) 화면 주소. 화면은 빈 틀만 내려가고, 데이터는 화면의 JS 가 /api/public/** 로 불러 그립니다.
 *
 * <pre>
 * GET /           SCR-GST-01 숙소 소개   → templates/guest/intro.html
 * GET /guidebook  SCR-GST-02 가이드북 열람 → templates/guest/guidebook.html
 * GET /reserve    SCR-GST-03 예약하기     → templates/guest/reserve.html (?checkIn=&checkOut=&guests= 가 있으면 바로 조회)
 * </pre>
 * 로그인 확인은 /api/**·/admin/** 에만 걸리므로 이 주소들은 로그인 없이 열립니다(WebConfig).
 */
@Controller
public class HomeController {

	@GetMapping("/")
	public String intro() {
		return "guest/intro";
	}

	@GetMapping("/guidebook")
	public String guidebook() {
		return "guest/guidebook";
	}

	@GetMapping("/reserve")
	public String reserve() {
		return "guest/reserve";
	}
}
