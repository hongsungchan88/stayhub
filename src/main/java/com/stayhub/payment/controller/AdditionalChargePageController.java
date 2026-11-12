package com.stayhub.payment.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 추가 요금 관리자 화면 (SCR-PAY-02 추가 요금 등록).
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>주소와 화면 파일을 이어 줍니다. 그게 전부입니다.
 * Model 에 데이터를 담지 않습니다. 화면의 자바스크립트가 {@code /api/additional-charges} 를 불러 그립니다.
 * 화면목록정의서의 유형이 "팝업"이라, 등록·수정은 목록 화면 안의 팝업으로 합니다.
 *
 * <pre>
 * GET /admin/additional-charges    추가 요금 목록 + 등록·수정 팝업 → templates/admin/additional-charges/list.html
 * </pre>
 */
@Controller
@RequestMapping("/admin/additional-charges") // AdminMenu.ADDITIONAL_CHARGE 의 url
public class AdditionalChargePageController {

	@GetMapping
	public String list() {
		return "admin/additional-charges/list";
	}
}
