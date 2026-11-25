package com.stayhub.statistics;

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
 * 대시보드 화면 SCR-STA-01·02 (/admin) 역할별 테스트.
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>HOST·STAFF 에게는 그래프·내보내기·Chart.js 가 나오고, FRONT·CLEANING 에게는 안내 문구만 나오며
 * 통계 API 를 부르는 script 가 HTML 에 아예 없는지 확인합니다(통계 PR 리뷰 "FRONT·CLEANING 이면 통계 API 를 부르지 않게").
 * 로그인은 AdminPageTest 와 같은 방법으로 세션에 {@link LoginUser} 를 직접 넣습니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DashboardPageTest {

	private static final String GUIDE = "왼쪽 메뉴에서 업무를 선택하세요.";
	private static final String CHART_JS = "chart.js@2.9.4";

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("HOST·STAFF 는 그래프 3종·내보내기 팝업·Chart.js 가 나오고 안내 문구는 없다")
	void hostAndStaffSeeDashboard() throws Exception {
		for (UserRole role : List.of(UserRole.HOST, UserRole.STAFF)) {
			mockMvc.perform(get("/admin").session(sessionOf(role)))
					.andExpect(status().isOk())
					.andExpect(view().name("admin/dashboard"))
					.andExpect(content().string(Matchers.containsString(CHART_JS)))
					.andExpect(content().string(Matchers.containsString("id=\"occupancyChart\"")))
					.andExpect(content().string(Matchers.containsString("id=\"channelChart\"")))
					.andExpect(content().string(Matchers.containsString("id=\"revenueChart\"")))
					.andExpect(content().string(Matchers.containsString("id=\"exportModal\"")))
					.andExpect(content().string(Matchers.containsString("/api/statistics")))
					.andExpect(content().string(Matchers.not(Matchers.containsString(GUIDE))));
		}
	}

	@Test
	@DisplayName("FRONT·CLEANING 은 안내 문구만 — 그래프·버튼·Chart.js·통계 API 주소가 HTML 에 없다")
	void otherRolesSeeGuideOnly() throws Exception {
		for (UserRole role : List.of(UserRole.FRONT, UserRole.CLEANING)) {
			mockMvc.perform(get("/admin").session(sessionOf(role)))
					.andExpect(status().isOk())
					.andExpect(content().string(Matchers.containsString(GUIDE)))
					.andExpect(content().string(Matchers.not(Matchers.containsString(CHART_JS))))
					.andExpect(content().string(Matchers.not(Matchers.containsString("occupancyChart"))))
					.andExpect(content().string(Matchers.not(Matchers.containsString("exportModal"))))
					.andExpect(content().string(Matchers.not(Matchers.containsString("/api/statistics"))));
		}
	}

	/** AdminPageTest 의 sessionOf 와 같은 방법 (AdminPageTest 는 그대로 둠) */
	private MockHttpSession sessionOf(UserRole role) {
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(LoginUser.SESSION_KEY, new LoginUser(1L, "tester", "테스트사용자", role));
		return session;
	}
}
