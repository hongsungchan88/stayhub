package com.stayhub.room.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 객실 관리자 화면 (SCR-ROM-01 객실 현황 보드, SCR-ROM-02 객실 상태 변경 팝업).
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>주소와 화면 파일을 이어 줍니다. 그게 전부입니다.
 * Model 에 데이터를 담지 않습니다. 화면의 자바스크립트가 {@code /api/rooms} 를 불러 그립니다.
 * 상태 변경(SCR-ROM-02)은 따로 주소가 없는 팝업이라 같은 화면 파일 안에 있습니다.
 *
 * <pre>
 * GET /admin/rooms    객실 현황 보드 + 상태 변경 팝업 → templates/admin/rooms/list.html
 * </pre>
 * 객실 등록·수정 화면은 화면목록정의서 1차 범위에 없어서 만들지 않았습니다.
 */
@Controller
@RequestMapping("/admin/rooms") // AdminMenu.ROOM_BOARD 의 url
public class RoomPageController {

	@GetMapping
	public String list() {
		return "admin/rooms/list";
	}
}
