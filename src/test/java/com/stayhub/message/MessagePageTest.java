package com.stayhub.message;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
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

/** 통합 인박스 화면 SCR-COM-01. */
@SpringBootTest
@AutoConfigureMockMvc
class MessagePageTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("FRONT 로 로그인하면 인박스 화면, AI 초안 영역은 숨김")
    void opensInbox() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(LoginUser.SESSION_KEY, new LoginUser(1L, "front", "프런트", UserRole.FRONT));

        mockMvc.perform(get("/admin/messages").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/messages/inbox"))
                .andExpect(content().string(Matchers.containsString("id=\"aiDraft\"")));
    }

    @Test
    @DisplayName("로그인 없이 /admin/messages 는 /login?expired")
    void withoutLoginRedirects() throws Exception {
        mockMvc.perform(get("/admin/messages"))
                .andExpect(redirectedUrl("/login?expired"));
    }
}
