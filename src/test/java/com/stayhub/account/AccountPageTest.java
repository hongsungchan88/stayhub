package com.stayhub.account;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 로그인 전 계정 화면 테스트 — 비밀번호 찾기 SCR-ACC-03.
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>로그인 없이 화면이 열리는지, 주소에 token 이 있고 없음에 따라 알맞은 폼이 나오는지,
 * 로그인 화면에 [비밀번호 찾기] 링크가 있는지(화면 흐름도 FLW-01) 확인합니다.
 * API 동작은 PasswordResetServiceTest 가 확인합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AccountPageTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("로그인 없이 /password-reset 은 200, 아이디 또는 이메일 입력 폼만 보인다")
	void requestFormWithoutToken() throws Exception {
		mockMvc.perform(get("/password-reset"))
				.andExpect(status().isOk())
				.andExpect(view().name("password-reset"))
				.andExpect(content().string(Matchers.containsString("name=\"loginIdOrEmail\"")))
				.andExpect(content().string(Matchers.not(Matchers.containsString("name=\"newPassword\""))));
	}

	@Test
	@DisplayName("재설정 링크(?token=...)로 들어오면 새 비밀번호 폼만 보인다")
	void confirmFormWithToken() throws Exception {
		mockMvc.perform(get("/password-reset").param("token", "any-token"))
				.andExpect(status().isOk())
				.andExpect(content().string(Matchers.containsString("name=\"newPassword\"")))
				.andExpect(content().string(Matchers.containsString("id=\"newPasswordConfirm\"")))
				.andExpect(content().string(Matchers.not(Matchers.containsString("name=\"loginIdOrEmail\""))));
	}

	@Test
	@DisplayName("로그인 화면에 [비밀번호 찾기] 링크가 있다 (FLW-01)")
	void loginPageLinksToPasswordReset() throws Exception {
		mockMvc.perform(get("/login"))
				.andExpect(status().isOk())
				.andExpect(content().string(Matchers.containsString("href=\"/password-reset\"")));
	}
}
