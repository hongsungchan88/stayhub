package com.stayhub.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.stayhub.account.dto.LoginUser;
import com.stayhub.account.entity.UserRole;
import com.stayhub.channel.entity.Channel;
import com.stayhub.channel.repository.ChannelRepository;
import com.stayhub.payment.dto.ChannelCommissionUpdateRequest;
import com.stayhub.payment.dto.PaymentCreateRequest;
import com.stayhub.payment.dto.PaymentResponse;
import com.stayhub.payment.service.ChannelCommissionService;
import com.stayhub.payment.service.PaymentService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 채널 수수료 설정 테스트 (PAY-003, SCR-PAY-03). 수수료율 수정 API·권한·입력값 검사, 바꾼 비율이 새 결제에 쓰이는지, 화면이 그려지는지 확인합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ChannelCommissionTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ChannelRepository channelRepository;

	@Autowired
	private ChannelCommissionService channelCommissionService;

	@Autowired
	private PaymentService paymentService;

	@Autowired
	private EntityManager entityManager;

	@Test
	@DisplayName("호스트가 수수료율·기본 수수료율을 바꾸면 응답과 DB 에 새 값이 들어간다")
	void hostUpdatesRates() throws Exception {
		Long channelId = ChannelFixture.save(channelRepository, "10.00", true).getId();

		mockMvc.perform(put("/api/channel-commissions/" + channelId).session(sessionOf(UserRole.HOST))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"commissionRate\":12.5,\"defaultCommissionRate\":7.25}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(channelId))
				.andExpect(jsonPath("$.data.commissionRate").value(12.5))
				.andExpect(jsonPath("$.data.defaultCommissionRate").value(7.25))
				.andExpect(jsonPath("$.data.channelName").value("테스트 채널"));

		Channel saved = reload(channelId);
		assertThat(saved.getCommissionRate()).isEqualByComparingTo("12.50");
		assertThat(saved.getDefaultCommissionRate()).isEqualByComparingTo("7.25");
		assertThat(saved.isRegistered()).isTrue(); // 등록 여부는 그대로
	}

	@Test
	@DisplayName("호스트가 아니면 403 이고 값은 바뀌지 않는다")
	void nonHostForbidden() throws Exception {
		Long channelId = ChannelFixture.save(channelRepository, "10.00", true).getId();

		mockMvc.perform(put("/api/channel-commissions/" + channelId).session(sessionOf(UserRole.FRONT))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"commissionRate\":20,\"defaultCommissionRate\":20}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("권한이 없습니다."));

		assertThat(reload(channelId).getCommissionRate()).isEqualByComparingTo("10.00");
	}

	@Test
	@DisplayName("로그인하지 않으면 401")
	void withoutLogin401() throws Exception {
		mockMvc.perform(put("/api/channel-commissions/1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"commissionRate\":20,\"defaultCommissionRate\":20}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("100 초과·음수·소수 셋째 자리·빈칸은 400")
	void invalidRatesRejected() throws Exception {
		Long channelId = ChannelFixture.save(channelRepository, "10.00", true).getId();
		String[] bodies = {
				"{\"commissionRate\":100.01,\"defaultCommissionRate\":10}",
				"{\"commissionRate\":10,\"defaultCommissionRate\":-1}",
				"{\"commissionRate\":10.125,\"defaultCommissionRate\":10}",
				"{\"commissionRate\":null,\"defaultCommissionRate\":10}",
		};
		for (String body : bodies) {
			mockMvc.perform(put("/api/channel-commissions/" + channelId).session(sessionOf(UserRole.HOST))
							.contentType(MediaType.APPLICATION_JSON)
							.content(body))
					.andExpect(status().isBadRequest());
		}
		assertThat(reload(channelId).getCommissionRate()).isEqualByComparingTo("10.00");
	}

	@Test
	@DisplayName("0 과 100 은 경계값이라 저장된다")
	void boundaryRatesAccepted() {
		Long channelId = ChannelFixture.save(channelRepository, "10.00", true).getId();

		var response = channelCommissionService.update(channelId,
				new ChannelCommissionUpdateRequest(new BigDecimal("0"), new BigDecimal("100")));

		assertThat(response.commissionRate()).isEqualByComparingTo("0");
		assertThat(response.defaultCommissionRate()).isEqualByComparingTo("100");
	}

	@Test
	@DisplayName("없는 채널이면 400 \"예약 채널을 찾을 수 없습니다.\"")
	void unknownChannel() throws Exception {
		mockMvc.perform(put("/api/channel-commissions/-1").session(sessionOf(UserRole.HOST))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"commissionRate\":10,\"defaultCommissionRate\":10}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("예약 채널을 찾을 수 없습니다."));
	}

	@Test
	@DisplayName("바꾼 수수료율은 그 뒤에 등록하는 결제에 쓰이고, 이미 저장된 결제는 그대로다")
	void newRateAppliesToNewPaymentsOnly() {
		Long registeredId = ChannelFixture.save(channelRepository, "10.00", true).getId();
		Long unregisteredId = ChannelFixture.save(channelRepository, "10.00", false).getId();
		PaymentResponse before = paymentService.create(paymentOf(registeredId));

		channelCommissionService.update(registeredId,
				new ChannelCommissionUpdateRequest(new BigDecimal("15.00"), new BigDecimal("10.00")));
		channelCommissionService.update(unregisteredId,
				new ChannelCommissionUpdateRequest(new BigDecimal("10.00"), new BigDecimal("5.00")));

		// 등록 채널은 수수료율, 미등록 채널은 기본 수수료율
		assertThat(paymentService.create(paymentOf(registeredId)).commissionAmount()).isEqualByComparingTo("15000.00");
		assertThat(paymentService.create(paymentOf(unregisteredId)).commissionAmount()).isEqualByComparingTo("5000.00");
		assertThat(paymentService.getDetail(before.id()).commissionAmount()).isEqualByComparingTo("10000.00");
	}

	@Test
	@DisplayName("화면: 로그인하지 않으면 /login?expired, 로그인하면 표와 수정 팝업이 그려진다")
	void pageRenders() throws Exception {
		mockMvc.perform(get("/admin/channel-commissions"))
				.andExpect(redirectedUrl("/login?expired"));

		mockMvc.perform(get("/admin/channel-commissions").session(sessionOf(UserRole.HOST)))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/channel-commissions/list"))
				.andExpect(content().string(containsString("채널 수수료 설정")))
				.andExpect(content().string(containsString("id=\"commissionModal\"")))
				.andExpect(content().string(containsString("name=\"commissionRate\"")))
				.andExpect(content().string(containsString("name=\"defaultCommissionRate\"")));
	}

	private PaymentCreateRequest paymentOf(Long channelId) {
		return new PaymentCreateRequest(1L, channelId, "사전 결제", new BigDecimal("100000.00"), null);
	}

	/** 영속성 컨텍스트를 비우고 DB 에서 다시 읽음 */
	private Channel reload(Long channelId) {
		entityManager.flush();
		entityManager.clear();
		return channelRepository.findById(channelId).orElseThrow();
	}

	private MockHttpSession sessionOf(UserRole role) {
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(LoginUser.SESSION_KEY, new LoginUser(1L, "tester", "테스트사용자", role));
		return session;
	}
}
