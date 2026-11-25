package com.stayhub.account.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 직원 계정 관리 화면 (SCR-ACC-04, ACC-005).
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>주소와 화면 파일을 이어 줍니다. 데이터는 화면의 자바스크립트가 {@code /api/users} 를 불러 그립니다.
 * 등록·수정은 목록 화면 안 팝업이라(화면 정의서 "목록 + 등록/수정 팝업") 편집 화면 주소가 없습니다.
 * 호스트가 아니면 {@code AdminPageInterceptor} 가 403 을 냅니다(AdminMenu.STAFF_ACCOUNT = HOST).
 *
 * <pre>
 * GET /admin/users    목록 + 등록/수정 팝업 → templates/admin/users/list.html
 * </pre>
 */
@Controller
@RequestMapping("/admin/users")
public class UserPageController {

    @GetMapping
    public String list(){
        return "admin/users/list";
    }
}
