package com.stayhub.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stayhub.account.dto.LoginUser;
import com.stayhub.account.entity.User;
import com.stayhub.account.entity.UserRole;
import com.stayhub.account.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인·로그아웃·401 API 테스트 (ACC-002·004).
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>앱 전체(인터셉터·예외 처리기 포함)를 띄우고 가짜 요청(MockMvc)으로 확인합니다.
 * 세션은 {@link MockHttpSession} 으로 들고 다닙니다. 브라우저의 JSESSIONID 쿠키 역할입니다.
 * 로그인이 필요한 API 의 예로 {@code GET /api/rooms} 를 씁니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthApiTest {

	private static final String LOGIN_ID = "authtest01";
	private static final String PASSWORD = "pass1234!";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void setUp() {
		userRepository.save(User.create(
				LOGIN_ID, passwordEncoder.encode(PASSWORD), "인증테스터", "010-0000-0000",
				"authtest01@test.com", UserRole.HOST));
	}

	@Test
	@DisplayName("로그인 없이 /api/rooms 를 부르면 401")
	void withoutLoginIs401() throws Exception {
		mockMvc.perform(get("/api/rooms"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
	}

	@Test
	@DisplayName("로그인하면 세션 ID 가 바뀌고, 그 세션으로 /api/rooms·/api/auth/me 가 200")
	void loginIssuesNewSession() throws Exception {
		MockHttpSession before = new MockHttpSession();

		MockHttpSession after = login(before);

		assertThat(before.isInvalid()).isTrue(); // 로그인 전 세션은 버려졌다
		assertThat(after.getId()).isNotEqualTo(before.getId());
		assertThat(after.getAttribute(LoginUser.SESSION_KEY))
				.isInstanceOfSatisfying(LoginUser.class, user -> assertThat(user.loginId()).isEqualTo(LOGIN_ID));

		mockMvc.perform(get("/api/rooms").session(after))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/auth/me").session(after))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.name").value("인증테스터"))
				.andExpect(jsonPath("$.data.role").value("HOST"));
	}

	@Test
	@DisplayName("로그아웃하면 쿠키가 지워지고, 같은 세션으로 다시 부르면 401")
	void logoutThen401() throws Exception {
		MockHttpSession session = login(new MockHttpSession());

		mockMvc.perform(post("/api/auth/logout").session(session))
				.andExpect(status().isOk())
				.andExpect(cookie().maxAge("JSESSIONID", 0));

		mockMvc.perform(get("/api/rooms").session(session))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("같은 계정이 다른 곳에서 로그인하면 첫 세션은 401, 새 세션은 200")
	void secondLoginKicksFirstSession() throws Exception {
		MockHttpSession first = login(new MockHttpSession());
		MockHttpSession second = login(new MockHttpSession());

		mockMvc.perform(get("/api/rooms").session(first))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/rooms").session(second))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("틀린 비밀번호는 400 이고 세션에 로그인 정보가 들어가지 않는다")
	void wrongPasswordIs400() throws Exception {
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginJson("wrong1234!")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("아이디 또는 비밀번호가 올바르지 않습니다."))
				.andExpect(request().sessionAttributeDoesNotExist(LoginUser.SESSION_KEY));
	}

	/** {@code session} 을 들고 로그인하고, 로그인 후의 (새) 세션을 돌려줍니다. */
	private MockHttpSession login(MockHttpSession session) throws Exception {
		return (MockHttpSession) mockMvc.perform(post("/api/auth/login")
						.session(session)
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginJson(PASSWORD)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.loginId").value(LOGIN_ID))
				.andReturn()
				.getRequest()
				.getSession(false);
	}

	private String loginJson(String password) {
		return "{\"loginId\":\"" + LOGIN_ID + "\",\"password\":\"" + password + "\"}";
	}
}
