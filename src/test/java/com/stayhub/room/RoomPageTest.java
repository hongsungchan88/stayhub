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
 * 객실 현황 보드 화면 테스트 (SCR-ROM-01·02).
 *
 * <p>화면 파일이 레이아웃과 함께 그려지는지만 확인합니다. 표의 내용은 화면의 자바스크립트가 {@code /api/rooms} 로 불러오므로,
 * API 동작은 {@code RoomServiceTest}·{@code RoomStatusApiTest} 가 확인합니다.
 * 로그인은 {@code AdminPageTest} 처럼 세션에 {@link LoginUser} 를 직접 넣습니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RoomPageTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("로그인 없이 /admin/rooms 면 /login?expired 로 이동")
	void withoutLoginRedirects() throws Exception {
		mockMvc.perform(get("/admin/rooms"))
				.andExpect(redirectedUrl("/login?expired"));
	}

	@Test
	@DisplayName("로그인하면 객실 현황 보드와 상태 변경 팝업이 그려진다")
	void boardRenders() throws Exception {
		mockMvc.perform(get("/admin/rooms").session(sessionOf(UserRole.HOST)))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/rooms/list"))
				.andExpect(content().string(Matchers.containsString("객실 현황 보드")))
				.andExpect(content().string(Matchers.containsString("id=\"statusModal\"")))
				.andExpect(content().string(Matchers.containsString("name=\"previousStatus\"")));
	}

	@Test
	@DisplayName("청소 담당(CLEANING)도 객실 현황 보드를 연다 (사이트맵: 호스트·프런트·청소)")
	void cleaningStaffCanOpen() throws Exception {
		mockMvc.perform(get("/admin/rooms").session(sessionOf(UserRole.CLEANING)))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/rooms/list"));
	}

	private MockHttpSession sessionOf(UserRole role) {
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(LoginUser.SESSION_KEY, new LoginUser(1L, "tester", "테스트사용자", role));
		return session;
	}
}
