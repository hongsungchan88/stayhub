package com.stayhub.payment.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 채널 수수료 설정 관리자 화면 (SCR-PAY-03).
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>주소와 화면 파일을 이어 줍니다. 그게 전부입니다.
 * Model 에 데이터를 담지 않습니다. 화면의 자바스크립트가 {@code /api/channels} 로 목록을 그리고,
 * {@code /api/channel-commissions/{id}} 로 수수료율을 고칩니다.
 *
 * <pre>
 * GET /admin/channel-commissions    채널 목록 + 수수료율 수정 팝업 → templates/admin/channel-commissions/list.html
 * </pre>
 */
@Controller
@RequestMapping("/admin/channel-commissions") // AdminMenu.CHANNEL_COMMISSION 의 url
public class ChannelCommissionPageController {

	@GetMapping
	public String list() {
		return "admin/channel-commissions/list";
	}
}
