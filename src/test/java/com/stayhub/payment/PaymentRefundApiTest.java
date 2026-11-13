package com.stayhub.payment;

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
 * 환불 API 테스트 (PAY-001). 로그인한 뒤 실제 URL 로 요청해서 응답 모양을 확인합니다.
 * 계산 규칙 자체는 {@link PaymentRefundTest} 에서 확인합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PaymentRefundApiTest {

	private static final String LOGIN_ID = "refundapi01";
	private static final String PASSWORD = "pass1234!";

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

	private MockHttpSession session;
	private Long paymentId;

	@BeforeEach
	void setUp() throws Exception {
		userRepository.save(User.create(
				LOGIN_ID, passwordEncoder.encode(PASSWORD), "환불테스터", "010-0000-0000",
				"refundapi01@test.com", UserRole.HOST));
		session = (MockHttpSession) mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"loginId\":\"" + LOGIN_ID + "\",\"password\":\"" + PASSWORD + "\"}"))
				.andExpect(status().isOk())
				.andReturn().getRequest().getSession(false);

		Long channelId = ChannelFixture.save(channelRepository, "10.00", true).getId();
		Long roomId = roomRepository.save(Room.create(
				PropertyScope.DEFAULT_PROPERTY_ID, "REFUNDAPI-101", "STANDARD", new BigDecimal("100000.00"), 2)).getId();
		LocalDate checkIn = LocalDate.now().plusDays(10);
		Long reservationId = reservationService.create(
				null, "환불테스트", "010-0000-0000", roomId, null, checkIn, checkIn.plusDays(1), null).getId();
		paymentId = paymentService.create(new PaymentCreateRequest(
				reservationId, channelId, "사전 결제", new BigDecimal("100000.00"), null)).id();
	}

	@Test
	@DisplayName("환불 요청 → 환불 처리: 전액 환불 결과가 응답에 담긴다")
	void requestThenRefund() throws Exception {
		mockMvc.perform(put("/api/payments/{id}/refund-request", paymentId).session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.paymentStatus").value("REFUND_REQUESTED"));

		mockMvc.perform(put("/api/payments/{id}/refund", paymentId).session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.paymentStatus").value("REFUNDED"))
				.andExpect(jsonPath("$.data.refundRuleApplied").value("FULL"))
				.andExpect(jsonPath("$.data.refundAmount").value(100000.00));
	}

	@Test
	@DisplayName("요청 없이 환불 처리하면 400 과 안내 문구")
	void refundWithoutRequestIs400() throws Exception {
		mockMvc.perform(put("/api/payments/{id}/refund", paymentId).session(session))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("환불 요청 상태의 결제만 환불할 수 있습니다."));
	}

	@Test
	@DisplayName("거절 사유를 비우면 400")
	void rejectWithoutReasonIs400() throws Exception {
		mockMvc.perform(put("/api/payments/{id}/refund-request", paymentId).session(session))
				.andExpect(status().isOk());

		mockMvc.perform(put("/api/payments/{id}/refund-rejection", paymentId).session(session)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"reason\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false));
	}

	@Test
	@DisplayName("거절하면 결제 완료로 돌아가고 사유가 남는다")
	void rejectRefund() throws Exception {
		mockMvc.perform(put("/api/payments/{id}/refund-request", paymentId).session(session))
				.andExpect(status().isOk());

		mockMvc.perform(put("/api/payments/{id}/refund-rejection", paymentId).session(session)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"reason\":\"규정 외 요청\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.paymentStatus").value("COMPLETED"))
				.andExpect(jsonPath("$.data.refundRuleApplied").value("NONE"))
				.andExpect(jsonPath("$.data.errorNote").value("규정 외 요청"));
	}
}
