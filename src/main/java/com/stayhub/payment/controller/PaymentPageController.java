package com.stayhub.payment.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 결제/환불 관리자 화면 (SCR-PAY-01 결제 / 환불 내역).
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>주소와 화면 파일을 이어 줍니다. 그게 전부입니다.
 * Model 에 데이터를 담지 않습니다. 화면의 자바스크립트가 {@code /api/payments} 를 불러 그립니다.
 * 환불 요청·처리·거절은 따로 주소가 없는 팝업이라 같은 화면 파일 안에 있습니다.
 *
 * <pre>
 * GET /admin/payments    결제 / 환불 내역 + 환불 팝업 → templates/admin/payments/list.html
 * </pre>
 * 결제 등록·수정 화면은 화면목록정의서 1차 범위에 없어서 만들지 않았습니다.
 */
@Controller
@RequestMapping("/admin/payments") // AdminMenu.PAYMENT_LIST 의 url
public class PaymentPageController {

	@GetMapping
	public String list() {
		return "admin/payments/list";
	}
}
