package com.stayhub.statistics;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stayhub.account.dto.LoginUser;
import com.stayhub.account.entity.UserRole;
import java.time.LocalDate;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 통계 API 권한·CSV 응답 테스트 (STA-001~004).
 *
 * <p>계산은 StatisticsServiceTest 가 확인하고, 여기서는 "누가 부를 수 있는지"와 CSV 응답 모양만 봅니다.
 * 로그인은 AdminPageTest 처럼 세션에 {@link LoginUser} 를 직접 넣습니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StatisticsControllerTest {

	private static final String TODAY = LocalDate.now().toString();

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("HOST·STAFF 는 통계를 볼 수 있다")
	void hostAndStaffCanRead() throws Exception {
		mockMvc.perform(get("/api/statistics/revenue").param("from", TODAY).param("to", TODAY)
						.session(sessionOf(UserRole.HOST)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));
		mockMvc.perform(get("/api/statistics/occupancy").param("from", TODAY).param("to", TODAY)
						.session(sessionOf(UserRole.STAFF)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));
	}

	@Test
	@DisplayName("FRONT·CLEANING 은 403 \"권한이 없습니다.\"")
	void otherRolesAreForbidden() throws Exception {
		mockMvc.perform(get("/api/statistics/channels").param("from", TODAY).param("to", TODAY)
						.session(sessionOf(UserRole.FRONT)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("권한이 없습니다."));
		mockMvc.perform(get("/api/statistics/export").param("type", "REVENUE").param("from", TODAY).param("to", TODAY)
						.session(sessionOf(UserRole.CLEANING)))
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("export 는 CSV 파일로 내려받고, 맨 앞에 UTF-8 BOM 이 있다")
	void exportIsCsvFile() throws Exception {
		mockMvc.perform(get("/api/statistics/export").param("type", "OCCUPANCY").param("from", TODAY).param("to", TODAY)
						.session(sessionOf(UserRole.HOST)))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", Matchers.startsWith("text/csv")))
				.andExpect(header().string("Content-Disposition",
						Matchers.containsString("statistics-occupancy-" + TODAY + "-" + TODAY + ".csv")))
				.andExpect(content().string(Matchers.startsWith("\uFEFF시작일")));
	}

	@Test
	@DisplayName("날짜 형식이 틀리면 400")
	void wrongDateIs400() throws Exception {
		mockMvc.perform(get("/api/statistics/revenue").param("from", "2026/11/01").param("to", TODAY)
						.session(sessionOf(UserRole.HOST)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("from 값이 올바르지 않습니다."));
	}

	private MockHttpSession sessionOf(UserRole role) {
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(LoginUser.SESSION_KEY, new LoginUser(1L, "tester", "테스트사용자", role));
		return session;
	}
}
