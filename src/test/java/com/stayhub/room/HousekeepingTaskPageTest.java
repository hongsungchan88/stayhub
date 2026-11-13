package com.stayhub.room;

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
 * 청소 스케줄 화면 테스트 (SCR-ROM-05).
 *
 * <p>화면 파일이 레이아웃과 함께 그려지는지만 확인합니다. 작업 등록·상태 변경은 화면의 자바스크립트가 API 로 처리하므로
 * {@code HousekeepingTaskServiceTest} 가 확인합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class HousekeepingTaskPageTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("로그인 없이 /admin/housekeeping-tasks 면 /login?expired 로 이동")
	void withoutLoginRedirects() throws Exception {
		mockMvc.perform(get("/admin/housekeeping-tasks"))
				.andExpect(redirectedUrl("/login?expired"));
	}

	@Test
	@DisplayName("청소 담당(CLEANING)으로 로그인하면 청소 스케줄과 팝업 두 개가 그려진다")
	void scheduleRenders() throws Exception {
		mockMvc.perform(get("/admin/housekeeping-tasks").session(sessionOf(UserRole.CLEANING)))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/housekeeping-tasks/list"))
				.andExpect(content().string(Matchers.containsString("청소 스케줄")))
				.andExpect(content().string(Matchers.containsString("id=\"taskModal\"")))
				.andExpect(content().string(Matchers.containsString("id=\"statusModal\"")))
				.andExpect(content().string(Matchers.containsString("name=\"scheduledAt\"")));
	}

	private MockHttpSession sessionOf(UserRole role) {
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(LoginUser.SESSION_KEY, new LoginUser(1L, "tester", "테스트사용자", role));
		return session;
	}
}
