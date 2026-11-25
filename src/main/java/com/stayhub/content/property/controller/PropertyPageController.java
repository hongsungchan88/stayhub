package com.stayhub.content.property.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 소개페이지 편집 관리자 화면 (SCR-CMS-01).
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>주소와 화면 파일을 이어 줍니다. Model 에 데이터를 담지 않습니다.
 * 화면의 자바스크립트가 {@code /api/properties/1}·{@code /api/facilities}·{@code /api/property-facilities}·{@code /api/property-images} 를 불러 그립니다.
 * 로그인 확인·역할(HOST 만, AdminMenu PROPERTY)·레이아웃은 {@code AdminPageInterceptor} 가 합니다.
 *
 * <p>숙소는 1번 하나뿐이라(PropertyScope.DEFAULT_PROPERTY_ID) 목록 없이 편집 화면 하나입니다.
 *
 * <pre>
 * GET /admin/properties    편집 → templates/admin/properties/form.html
 * </pre>
 */
@Controller
@RequestMapping("/admin/properties")
public class PropertyPageController {

	@GetMapping
	public String edit() {
		return "admin/properties/form";
	}
}
