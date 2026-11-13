package com.stayhub.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.stayhub.account.dto.LoginUser;
import com.stayhub.account.entity.UserRole;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 관리자 화면 로그인 확인·역할별 메뉴 테스트 (W-314).
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>앱 전체를 띄우고 가짜 요청(MockMvc)으로 화면 주소를 불러, 리다이렉트·화면 이름·그려진 메뉴를 확인합니다.
 * 로그인은 API 를 거치지 않고 세션에 {@link LoginUser} 를 직접 넣습니다(로그인 API 자체는 AuthApiTest 가 확인).
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminPageTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("로그인 없이 /admin 이면 /login?expired 로 이동")
	void adminWithoutLoginRedirectsToLogin() throws Exception {
		mockMvc.perform(get("/admin"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login?expired"));
		mockMvc.perform(get("/admin/guidebooks"))
				.andExpect(redirectedUrl("/login?expired"));
	}

	@Test
	@DisplayName("로그인하면 /admin 은 200, 상단에 이름·역할")
	void adminWithLoginIs200() throws Exception {
		mockMvc.perform(get("/admin").session(sessionOf(UserRole.HOST)))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/dashboard"))
				.andExpect(content().string(Matchers.containsString("테스트사용자")));
	}

	@Test
	@DisplayName("/login 은 로그인 없이 200, ?expired 면 세션 만료 안내")
	void loginPageIsOpen() throws Exception {
		mockMvc.perform(get("/login"))
				.andExpect(status().isOk())
				.andExpect(content().string(Matchers.not(Matchers.containsString("세션이 만료되어"))));
		mockMvc.perform(get("/login?expired"))
				.andExpect(status().isOk())
				.andExpect(content().string(Matchers.containsString("세션이 만료되어 다시 로그인해 주세요.")));
	}

	@Test
	@DisplayName("HOST 는 결제·시스템관리 메뉴까지 전부 보인다")
	void hostSeesAllMenus() throws Exception {
		mockMvc.perform(get("/admin").session(sessionOf(UserRole.HOST)))
				.andExpect(content().string(Matchers.containsString("결제 / 환불 내역")))
				.andExpect(content().string(Matchers.containsString("직원 계정 관리")));

		assertThat(menusOf(UserRole.HOST)).containsExactly(AdminMenu.values());
		// 사이트맵 "접근 권한" 칸에 호스트가 없는 메뉴도 HOST 에게는 보인다
		assertThat(menusOf(UserRole.HOST)).contains(
				AdminMenu.CHECK_IN_OUT, AdminMenu.HOUSEKEEPING, AdminMenu.ADDITIONAL_CHARGE, AdminMenu.INBOX);
	}

	@Test
	@DisplayName("CLEANING 은 객실 현황·청소 스케줄만 보이고, 다른 메뉴는 HTML 에 없다")
	void cleaningSeesOnlyRoomMenus() throws Exception {
		assertThat(menusOf(UserRole.CLEANING)).containsExactly(AdminMenu.ROOM_BOARD, AdminMenu.HOUSEKEEPING);

		mockMvc.perform(get("/admin").session(sessionOf(UserRole.CLEANING)))
				.andExpect(content().string(Matchers.containsString("청소 스케줄")))
				.andExpect(content().string(Matchers.not(Matchers.containsString("결제 / 환불 내역"))))
				.andExpect(content().string(Matchers.not(Matchers.containsString("직원 계정 관리"))));
	}

	@Test
	@DisplayName("화면이 아직 없는 메뉴 주소는 준비 중 화면, 가이드북은 목록 화면")
	void notYetBuiltMenuShowsReady() throws Exception {
		MockHttpSession host = sessionOf(UserRole.HOST);
		// 예시 주소는 화면이 가장 늦게 생기는 메뉴(커뮤니케이션 W-306, 11/16~). /admin/payments 는 SCR-PAY-01 로 생겨서 바꿈
		mockMvc.perform(get("/admin/messages").session(host))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/ready"));
		mockMvc.perform(get("/admin/guidebooks").session(host))
				.andExpect(view().name("admin/guidebooks/list"));
		mockMvc.perform(get("/admin/guidebooks/3/edit").session(host))
				.andExpect(view().name("admin/guidebooks/form"));
	}

	@Test
	@DisplayName("/api/** 는 그대로 — 로그인 없이 부르면 리다이렉트가 아니라 401 JSON")
	void apiStill401() throws Exception {
		mockMvc.perform(get("/api/guidebooks"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
	}

	@Test
	@DisplayName("/403 은 403 안내 페이지")
	void forbiddenPage() throws Exception {
		mockMvc.perform(get("/403"))
				.andExpect(status().isForbidden())
				.andExpect(content().string(Matchers.containsString("접근 권한이 없습니다.")));
	}

	/** 화면에 그려진 메뉴 목록(그룹을 펼쳐서). */
	@SuppressWarnings("unchecked")
	private List<AdminMenu> menusOf(UserRole role) throws Exception {
		Map<String, List<AdminMenu>> groups = (Map<String, List<AdminMenu>>) mockMvc
				.perform(get("/admin").session(sessionOf(role)))
				.andReturn().getModelAndView().getModel().get("menuGroups");
		return groups.values().stream().flatMap(List::stream).toList();
	}

	private MockHttpSession sessionOf(UserRole role) {
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(LoginUser.SESSION_KEY, new LoginUser(1L, "tester", "테스트사용자", role));
		return session;
	}
}
