package com.stayhub.content.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 가이드북 관리자 화면 (SCR-CMS-02 목록, SCR-CMS-03 편집).
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>주소와 화면 파일(templates 아래 경로)을 이어 줍니다. 그게 전부입니다.
 * Model 에 데이터를 담지 않습니다. 화면의 자바스크립트가 {@code /api/guidebooks} 를 불러 그립니다.
 * 로그인 확인과 레이아웃(상단바·메뉴)은 {@code AdminPageInterceptor} 가 알아서 합니다.
 *
 * <pre>
 * GET /admin/guidebooks              목록   → templates/admin/guidebooks/list.html
 * GET /admin/guidebooks/new          등록   → templates/admin/guidebooks/form.html
 * GET /admin/guidebooks/{id}/edit    수정   → templates/admin/guidebooks/form.html (같은 파일, id 는 JS 가 주소에서 읽음)
 * </pre>
 *
 * <p><b>따라 만들 때 바꿀 곳</b>
 * <ul>
 *   <li>{@code @RequestMapping} 주소 — {@code AdminMenu} 에 적힌 내 메뉴 주소와 똑같이</li>
 *   <li>클래스명 — {@code Xxx + PageController}. API 컨트롤러({@code XxxController})와 같은 폴더에</li>
 *   <li>돌려주는 화면 경로 — {@code admin/내폴더/list} 처럼</li>
 *   <li>편집 화면이 없으면(목록만 있거나 팝업으로 편집) {@code create}·{@code edit} 메서드째 지웁니다</li>
 * </ul>
 */
@Controller
@RequestMapping("/admin/guidebooks") // ★ 바꿀 곳: 화면 주소 (AdminMenu 의 url 과 같게)
public class GuidebookPageController { // ★ 바꿀 곳: 클래스명

	@GetMapping
	public String list() {
		return "admin/guidebooks/list"; // ★ 바꿀 곳: 화면 경로
	}

	@GetMapping("/new") // ★ 바꿀 곳: 편집 화면이 없으면 메서드째 삭제
	public String create() {
		return "admin/guidebooks/form"; // ★ 바꿀 곳: 화면 경로
	}

	@GetMapping("/{id}/edit") // ★ 바꿀 곳: 편집 화면이 없으면 메서드째 삭제
	public String edit() {
		return "admin/guidebooks/form"; // ★ 바꿀 곳: 화면 경로
	}
}
