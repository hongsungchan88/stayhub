package com.stayhub.room.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 청소 스케줄 관리자 화면 (SCR-ROM-05).
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>주소와 화면 파일을 이어 줍니다. 그게 전부입니다.
 * Model 에 데이터를 담지 않습니다. 화면의 자바스크립트가 {@code /api/housekeeping-tasks} 를 불러 그립니다.
 * 작업 등록·재배정·상태 변경은 따로 주소가 없는 팝업이라 같은 화면 파일 안에 있습니다.
 *
 * <pre>
 * GET /admin/housekeeping-tasks    청소 스케줄 + 등록·재배정 팝업 → templates/admin/housekeeping-tasks/list.html
 * </pre>
 */
@Controller
@RequestMapping("/admin/housekeeping-tasks") // AdminMenu.HOUSEKEEPING 의 url
public class HousekeepingTaskPageController {

	@GetMapping
	public String list() {
		return "admin/housekeeping-tasks/list";
	}
}
