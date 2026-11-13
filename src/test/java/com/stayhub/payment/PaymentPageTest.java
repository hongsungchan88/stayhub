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
 * 결제 / 환불 내역 화면 테스트 (SCR-PAY-01).
 *
 * <p>화면 파일이 레이아웃과 함께 그려지는지만 확인합니다. 표의 내용과 환불 동작은 화면의 자바스크립트가 API 로 처리하므로
 * {@code PaymentServiceTest}·{@code PaymentRefundTest}·{@code PaymentRefundApiTest} 가 확인합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PaymentPageTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("로그인 없이 /admin/payments 면 /login?expired 로 이동")
	void withoutLoginRedirects() throws Exception {
		mockMvc.perform(get("/admin/payments"))
				.andExpect(redirectedUrl("/login?expired"));
	}

	@Test
	@DisplayName("로그인하면 결제 / 환불 내역과 환불 팝업이 그려진다")
	void listRenders() throws Exception {
		mockMvc.perform(get("/admin/payments").session(sessionOf(UserRole.FRONT)))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/payments/list"))
				.andExpect(content().string(Matchers.containsString("결제 / 환불 내역")))
				.andExpect(content().string(Matchers.containsString("id=\"refundModal\"")))
				.andExpect(content().string(Matchers.containsString("name=\"reason\"")));
	}

	private MockHttpSession sessionOf(UserRole role) {
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(LoginUser.SESSION_KEY, new LoginUser(1L, "tester", "테스트사용자", role));
		return session;
	}
}
