package com.stayhub.payment.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 영수증 / 세금계산서 관리자 화면 (SCR-PAY-04).
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>주소와 화면 파일을 이어 줍니다. 그게 전부입니다.
 * Model 에 데이터를 담지 않습니다. 화면의 자바스크립트가 {@code /api/tax-invoices} 를 불러 그립니다.
 * 화면목록정의서의 유형이 "팝업"이라, 발행 요청 등록·수정과 발행 상태 변경은 목록 화면 안의 팝업으로 합니다.
 *
 * <pre>
 * GET /admin/tax-invoices    발행 목록 + 등록·수정·상태 변경 팝업 → templates/admin/tax-invoices/list.html
 * </pre>
 */
@Controller
@RequestMapping("/admin/tax-invoices") // AdminMenu.TAX_INVOICE 의 url
public class TaxInvoicePageController {

	@GetMapping
	public String list() {
		return "admin/tax-invoices/list";
	}
}
