package com.stayhub.account.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 로그인 전 계정 화면: 비밀번호 찾기(SCR-ACC-03).
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>화면 이름(templates 아래 경로)만 돌려줍니다. 데이터는 화면의 자바스크립트가 {@code /api/password-reset/**} 로 보냅니다.
 * {@code /admin/**} 밖의 주소라 로그인 없이 열립니다(WebConfig 변경 없음).
 * 로그인 화면({@code /login})은 공통 {@code AdminController} 에 있고, 이 화면도 같은 단독 화면 모양입니다(레이아웃 없음).
 *
 * <pre>
 * GET /password-reset              ① 아이디 또는 이메일로 재설정 링크 요청
 * GET /password-reset?token=...    ② 재설정 링크로 들어와 새 비밀번호 저장
 * </pre>
 */
@Controller
public class AccountPageController {

	@GetMapping("/password-reset")
	public String passwordReset() {
		return "password-reset";
	}
}
