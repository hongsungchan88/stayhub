package com.stayhub.common.controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 관리자 공통 화면: 로그인(SCR-ACC-01), 403 안내, 대시보드(SCR-STA-01 자리), 준비 중.
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>화면 이름(templates 아래 경로)만 돌려줍니다. 데이터는 화면의 자바스크립트가 {@code /api/**} 로 가져옵니다.
 * 레이아웃이 쓰는 로그인 사용자·메뉴는 {@code AdminPageInterceptor} 가 넣습니다.
 *
 * <p><b>따라 만들 때 바꿀 곳</b>
 * <br>여기에 내 화면을 추가하지 마세요. 내 도메인의 {@code controller} 폴더에 {@code XxxPageController} 를 만듭니다.
 * 예) {@code content/controller/GuidebookPageController.java}
 */
@Controller
public class AdminController {

	@GetMapping("/login")
	public String login() {
		return "login";
	}

	/** 권한 없는 화면에 들어왔을 때. stayhub.js 의 api() 가 403 을 받으면 여기로 보냅니다. */
	@GetMapping("/403")
	@ResponseStatus(HttpStatus.FORBIDDEN)
	public String forbidden() {
		return "error/403";
	}

	@GetMapping("/admin")
	public String dashboard() {
		return "admin/dashboard";
	}

	/**
	 * 아직 컨트롤러가 없는 {@code /admin/...} 주소는 전부 "준비 중" 화면.
	 * 더 구체적인 주소(예: {@code /admin/guidebooks})를 가진 컨트롤러가 생기면 그쪽이 먼저 선택됩니다.
	 * shortcut: 주소 오타도 404 대신 "준비 중" 이 뜹니다. 1차 30화면이 다 생기면 이 메서드를 지웁니다.
	 */
	@GetMapping("/admin/**")
	public String ready() {
		return "admin/ready";
	}
}
