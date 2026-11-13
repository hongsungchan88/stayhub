package com.stayhub.payment;

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

/**
 * 추가 요금 등록 화면 테스트 (SCR-PAY-02).
 *
 * <p>화면 파일이 레이아웃과 함께 그려지는지만 확인합니다. 등록·수정·합산 규칙은 화면의 자바스크립트가 API 로 처리하므로
 * {@code AdditionalChargeServiceTest} 가 확인합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdditionalChargePageTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("로그인 없이 /admin/additional-charges 면 /login?expired 로 이동")
	void withoutLoginRedirects() throws Exception {
		mockMvc.perform(get("/admin/additional-charges"))
				.andExpect(redirectedUrl("/login?expired"));
	}

	@Test
	@DisplayName("프런트(FRONT)로 로그인하면 추가 요금 목록과 등록 팝업이 그려진다")
	void listRenders() throws Exception {
		mockMvc.perform(get("/admin/additional-charges").session(sessionOf(UserRole.FRONT)))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/additional-charges/list"))
				.andExpect(content().string(Matchers.containsString("추가 요금 등록")))
				.andExpect(content().string(Matchers.containsString("id=\"chargeModal\"")))
				.andExpect(content().string(Matchers.containsString("name=\"paymentId\"")));
	}

	private MockHttpSession sessionOf(UserRole role) {
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(LoginUser.SESSION_KEY, new LoginUser(1L, "tester", "테스트사용자", role));
		return session;
	}
}
