package com.stayhub.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stayhub.account.entity.User;
import com.stayhub.account.entity.UserRole;
import com.stayhub.account.repository.UserRepository;
import com.stayhub.channel.repository.ChannelRepository;
import com.stayhub.common.PropertyScope;
import com.stayhub.payment.dto.PaymentCreateRequest;
import com.stayhub.payment.repository.PaymentRepository;
import com.stayhub.payment.service.PaymentService;
import com.stayhub.reservation.service.ReservationService;
import com.stayhub.room.entity.Room;
import com.stayhub.room.repository.RoomRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
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
 * 결제 수단 검증 API 테스트 (PAY-001). 결제 등록·수정은 결제 수단으로 "사전 결제"·"현장 결제"만 받습니다.
 * 검증은 요청 DTO에 있어서 서비스가 아니라 실제 URL로 요청해 확인합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PaymentMethodApiTest {

	private static final String LOGIN_ID = "paymethod01";
	private static final String PASSWORD = "pass1234!";
	private static final String INVALID_MESSAGE = "paymentMethod: 결제 수단은 사전 결제 또는 현장 결제만 입력할 수 있습니다.";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private RoomRepository roomRepository;

	@Autowired
	private ChannelRepository channelRepository;

	@Autowired
	private ReservationService reservationService;

	@Autowired
	private PaymentService paymentService;

	@Autowired
	private PaymentRepository paymentRepository;

	private MockHttpSession session;
	private Long reservationId;
	private Long channelId;

	@BeforeEach
	void setUp() throws Exception {
		userRepository.save(User.create(
				LOGIN_ID, passwordEncoder.encode(PASSWORD), "결제수단테스터", "010-0000-0000",
				"paymethod01@test.com", UserRole.HOST));
		session = (MockHttpSession) mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"loginId\":\"" + LOGIN_ID + "\",\"password\":\"" + PASSWORD + "\"}"))
				.andExpect(status().isOk())
				.andReturn().getRequest().getSession(false);

		channelId = ChannelFixture.save(channelRepository, "10.00", true).getId();
		Long roomId = roomRepository.save(Room.create(
				PropertyScope.DEFAULT_PROPERTY_ID, "PAYMETHOD-101", "STANDARD", new BigDecimal("100000.00"), 2)).getId();
		LocalDate checkIn = LocalDate.now().plusDays(10);
		reservationId = reservationService.create(
				null, "결제수단테스트", "010-0000-0000", roomId, null, checkIn, checkIn.plusDays(1), null).getId();
	}

	@Test
	@DisplayName("등록: 사전 결제·현장 결제는 받는다")
	void createWithAllowedMethods() throws Exception {
		for (String method : new String[] {"사전 결제", "현장 결제"}) {
			mockMvc.perform(post("/api/payments").session(session)
							.contentType(MediaType.APPLICATION_JSON)
							.content(body(method)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.data.paymentMethod").value(method));
		}
	}

	@Test
	@DisplayName("등록: 그 밖의 값은 400과 안내 문구, 결제가 생기지 않는다")
	void createWithOtherMethodIs400() throws Exception {
		long before = paymentRepository.count();
		for (String method : new String[] {"카드", "CARD", "사전결제", " 사전 결제"}) {
			mockMvc.perform(post("/api/payments").session(session)
							.contentType(MediaType.APPLICATION_JSON)
							.content(body(method)))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.message").value(INVALID_MESSAGE));
		}
		assertThat(paymentRepository.count()).isEqualTo(before);
	}

	@Test
	@DisplayName("수정: 그 밖의 값은 400이고 결제 수단은 그대로다")
	void updateWithOtherMethodIs400() throws Exception {
		Long paymentId = paymentService.create(new PaymentCreateRequest(
				reservationId, channelId, "사전 결제", new BigDecimal("100000.00"), null)).id();

		mockMvc.perform(put("/api/payments/{id}", paymentId).session(session)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body("계좌 이체")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(INVALID_MESSAGE));
		assertThat(paymentService.getDetail(paymentId).paymentMethod()).isEqualTo("사전 결제");

		mockMvc.perform(put("/api/payments/{id}", paymentId).session(session)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body("현장 결제")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.paymentMethod").value("현장 결제"));
	}

	private String body(String method) {
		return "{\"reservationId\":" + reservationId + ",\"channelId\":" + channelId
				+ ",\"paymentMethod\":\"" + method + "\",\"paymentAmount\":100000}";
	}
}
