package com.stayhub.account;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.stayhub.account.dto.LoginUser;
import com.stayhub.account.entity.UserRole;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 직원 계정 관리 화면 테스트 (SCR-ACC-04). 목록·팝업이 그려지는지, 호스트만 열리는지 확인합니다.
 * API 동작은 UserServiceTest 가 확인합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class UserPageTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("HOST 는 /admin/users 에서 목록 화면과 등록/수정 팝업을 본다")
    void hostSeesUserPage() throws Exception {
        mockMvc.perform(get("/admin/users").session(sessionOf(UserRole.HOST)))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users/list"))
                .andExpect(content().string(Matchers.containsString("id=\"userModal\"")));
    }

    @Test
    @DisplayName("HOST 가 아니면 /admin/users 는 403")
    void otherRolesAreForbidden() throws Exception {
        for (UserRole role : new UserRole[] {UserRole.FRONT, UserRole.CLEANING, UserRole.STAFF}) {
            mockMvc.perform(get("/admin/users").session(sessionOf(role)))
                    .andExpect(status().isForbidden());
        }
    }

    private MockHttpSession sessionOf(UserRole role) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(LoginUser.SESSION_KEY, new LoginUser(1L, "tester", "테스트사용자", role));
        return session;
    }
}