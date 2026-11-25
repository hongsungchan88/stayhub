package com.stayhub.content.property;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.stayhub.account.dto.LoginUser;
import com.stayhub.account.entity.UserRole;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 소개페이지 편집 화면 SCR-CMS-01 (/admin/properties) 테스트.
 *
 * <p>HOST 에게 편집 폼·사진 영역이 나오고, 다른 역할은 403(AdminMenu PROPERTY 는 HOST 만)인지 확인합니다.
 * 로그인은 AdminPageTest 와 같은 방법으로 세션에 {@link LoginUser} 를 직접 넣습니다. API 동작은 서비스 테스트가 확인합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PropertyPageTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("HOST 는 /admin/properties 가 200, 편집 폼·시설·사진 업로드 영역이 나온다")
	void hostSeesEditPage() throws Exception {
		mockMvc.perform(get("/admin/properties").session(sessionOf(UserRole.HOST)))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/properties/form"))
				.andExpect(content().string(Matchers.containsString("id=\"editForm\"")))
				.andExpect(content().string(Matchers.containsString("name=\"propertyName\"")))
				.andExpect(content().string(Matchers.containsString("name=\"description\"")))
				.andExpect(content().string(Matchers.containsString("name=\"status\"")))
				.andExpect(content().string(Matchers.containsString("id=\"facilities\"")))
				.andExpect(content().string(Matchers.containsString("id=\"imageFile\"")));
	}

	@Test
	@DisplayName("HOST 가 아니면 403 — FRONT·CLEANING·STAFF")
	void otherRolesAre403() throws Exception {
		for (UserRole role : List.of(UserRole.FRONT, UserRole.CLEANING, UserRole.STAFF)) {
			mockMvc.perform(get("/admin/properties").session(sessionOf(role)))
					.andExpect(status().isForbidden());
		}
	}

	/** AdminPageTest 의 sessionOf 와 같은 방법 */
	private MockHttpSession sessionOf(UserRole role) {
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(LoginUser.SESSION_KEY, new LoginUser(1L, "tester", "테스트사용자", role));
		return session;
	}
}
