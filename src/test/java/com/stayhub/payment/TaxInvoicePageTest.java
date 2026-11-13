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
 * 영수증 / 세금계산서 화면 테스트 (SCR-PAY-04).
 *
 * <p>화면 파일이 레이아웃과 함께 그려지는지만 확인합니다. 발행 규칙은 화면의 자바스크립트가 API 로 처리하므로
 * {@code TaxInvoiceServiceTest} 가 확인합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TaxInvoicePageTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("로그인 없이 /admin/tax-invoices 면 /login?expired 로 이동")
	void withoutLoginRedirects() throws Exception {
		mockMvc.perform(get("/admin/tax-invoices"))
				.andExpect(redirectedUrl("/login?expired"));
	}

	@Test
	@DisplayName("로그인하면 발행 목록과 등록·상태 변경 팝업이 그려진다")
	void listRenders() throws Exception {
		mockMvc.perform(get("/admin/tax-invoices").session(sessionOf(UserRole.HOST)))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/tax-invoices/list"))
				.andExpect(content().string(Matchers.containsString("영수증 / 세금계산서")))
				.andExpect(content().string(Matchers.containsString("id=\"invoiceModal\"")))
				.andExpect(content().string(Matchers.containsString("id=\"statusModal\"")))
				.andExpect(content().string(Matchers.containsString("name=\"businessNumber\"")));
	}

	private MockHttpSession sessionOf(UserRole role) {
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(LoginUser.SESSION_KEY, new LoginUser(1L, "tester", "테스트사용자", role));
		return session;
	}
}
