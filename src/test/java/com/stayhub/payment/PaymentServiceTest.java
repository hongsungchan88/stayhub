package com.stayhub.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stayhub.channel.repository.ChannelRepository;
import com.stayhub.common.BusinessException;
import com.stayhub.common.PageResponse;
import com.stayhub.payment.dto.AdditionalChargeCreateRequest;
import com.stayhub.payment.dto.PaymentCreateRequest;
import com.stayhub.payment.dto.PaymentResponse;
import com.stayhub.payment.dto.PaymentTotalResponse;
import com.stayhub.payment.dto.PaymentUpdateRequest;
import com.stayhub.payment.entity.AdditionalChargeType;
import com.stayhub.payment.entity.PaymentStatus;
import com.stayhub.payment.repository.PaymentRepository;
import com.stayhub.payment.service.AdditionalChargeService;
import com.stayhub.payment.service.PaymentService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제/환불 서비스 테스트.
 *
 * <p>실제 DB(application-local.yml 의 MySQL)에 붙어서 등록·조회·수정이 제대로 되는지 확인합니다.
 * {@code @Transactional} 이 붙어 있어서 테스트가 끝나면 넣었던 데이터가 전부 되돌려집니다(롤백).
 * 삭제 기능이 없는 테이블이라 삭제 테스트는 없습니다.
 */
@SpringBootTest
@Transactional
class PaymentServiceTest {

	@Autowired
	private PaymentService paymentService;

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private AdditionalChargeService additionalChargeService;

	@Autowired
	private ChannelRepository channelRepository;

	@BeforeEach
	void setUp() {
		// 개발 DB 에 이미 들어 있는 데이터 때문에 결과가 흔들리지 않도록 테스트 안에서만 비웁니다.
		// @Transactional 이라 테스트가 끝나면 지운 것까지 원래대로 돌아옵니다.
		// 경고: @Transactional을 지우면 개발 DB의 이 테이블이 실제로 비워집니다. 지우지 마세요.
		paymentRepository.deleteAll();
	}

	@Test
	@DisplayName("등록하면 목록에 나온다")
	void createThenAppearsInList() {
		PaymentResponse created = paymentService.create(createRequest(1L));

		PageResponse<PaymentResponse> all = paymentService.getList(0);
		assertThat(all.content()).extracting(PaymentResponse::id).containsExactly(created.id());

		// 상태는 COMPLETED, 환불 값은 비어 있다
		assertThat(created.paymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
		assertThat(created.refundAmount()).isNull();
		assertThat(created.refundRuleApplied()).isNull();
	}

	@Test
	@DisplayName("수정하면 바뀐 값이 조회된다")
	void updateIsApplied() {
		PaymentResponse created = paymentService.create(createRequest(1L));
		Long channelId = ChannelFixture.save(channelRepository, "10.00", true).getId();

		paymentService.update(created.id(), new PaymentUpdateRequest(
				2L, channelId, "현장 결제", new BigDecimal("200000.00"), "중복 결제 확인 요청"));

		PaymentResponse found = paymentService.getDetail(created.id());
		assertThat(found.reservationId()).isEqualTo(2L);
		assertThat(found.channelId()).isEqualTo(channelId);
		assertThat(found.paymentMethod()).isEqualTo("현장 결제");
		assertThat(found.paymentAmount()).isEqualByComparingTo("200000.00");
		assertThat(found.commissionAmount()).isEqualByComparingTo("20000.00");
		assertThat(found.netRevenue()).isEqualByComparingTo("180000.00");
		assertThat(found.errorNote()).isEqualTo("중복 결제 확인 요청");
	}

	@Test
	@DisplayName("없는 항목을 상세 조회하면 BusinessException 이 난다")
	void getMissingThrows() {
		assertThatThrownBy(() -> paymentService.getDetail(99999L))
				.isInstanceOf(BusinessException.class)
				.hasMessage("결제 내역을 찾을 수 없습니다.");
	}

	@Test
	@DisplayName("등록 채널은 채널 수수료율로 수수료·실매출을 계산한다")
	void registeredChannelUsesCommissionRate() {
		Long channelId = ChannelFixture.save(channelRepository, "15.00", true).getId();

		PaymentResponse created = paymentService.create(paymentRequest(channelId, "100000.00"));

		assertThat(created.commissionAmount()).isEqualByComparingTo("15000.00");
		assertThat(created.netRevenue()).isEqualByComparingTo("85000.00");
	}

	@Test
	@DisplayName("미등록 채널은 기본 수수료율 10% 로 계산한다")
	void unregisteredChannelUsesDefaultRate() {
		Long channelId = ChannelFixture.save(channelRepository, "15.00", false).getId();

		PaymentResponse created = paymentService.create(paymentRequest(channelId, "100000.00"));

		assertThat(created.commissionAmount()).isEqualByComparingTo("10000.00");
		assertThat(created.netRevenue()).isEqualByComparingTo("90000.00");
	}

	@Test
	@DisplayName("수수료는 소수 셋째 자리에서 반올림한다")
	void commissionIsRounded() {
		Long channelId = ChannelFixture.save(channelRepository, "3.33", true).getId();

		PaymentResponse created = paymentService.create(paymentRequest(channelId, "33333.33"));

		// 33333.33 x 3.33% = 1109.998889
		assertThat(created.commissionAmount()).isEqualByComparingTo("1110.00");
		assertThat(created.netRevenue()).isEqualByComparingTo("32223.33");
	}

	@Test
	@DisplayName("수정하면 바뀐 금액·채널로 수수료·실매출을 다시 계산한다")
	void updateRecalculatesCommission() {
		PaymentResponse created = paymentService.create(createRequest(1L));
		Long channelId = ChannelFixture.save(channelRepository, "20.00", true).getId();

		PaymentResponse updated = paymentService.update(created.id(), new PaymentUpdateRequest(
				1L, channelId, "사전 결제", new BigDecimal("200000.00"), null));

		assertThat(updated.commissionAmount()).isEqualByComparingTo("40000.00");
		assertThat(updated.netRevenue()).isEqualByComparingTo("160000.00");
	}

	@Test
	@DisplayName("없는 채널로 등록하면 BusinessException 이 난다")
	void missingChannelThrows() {
		assertThatThrownBy(() -> paymentService.create(paymentRequest(99999L, "100000.00")))
				.isInstanceOf(BusinessException.class)
				.hasMessage("예약 채널을 찾을 수 없습니다.");
	}

	@Test
	@DisplayName("결제 합계는 합산된 추가 요금만 더하고 결제 금액은 바꾸지 않는다")
	void totalAddsOnlyMergedCharges() {
		PaymentResponse payment = paymentService.create(createRequest(1L));
		additionalChargeService.create(chargeRequest("4000.00", true, payment.id()));
		additionalChargeService.create(chargeRequest("30000.00", true, payment.id()));
		additionalChargeService.create(chargeRequest("7000.00", false, null)); // 별도 청구

		PaymentTotalResponse total = paymentService.getTotal(payment.id());

		assertThat(total.paymentAmount()).isEqualByComparingTo("100000.00");
		assertThat(total.mergedChargeAmount()).isEqualByComparingTo("34000.00");
		assertThat(total.totalAmount()).isEqualByComparingTo("134000.00");
		assertThat(paymentService.getDetail(payment.id()).paymentAmount()).isEqualByComparingTo("100000.00");
	}

	@Test
	@DisplayName("합산된 추가 요금이 없으면 결제 합계는 결제 금액과 같다")
	void totalWithoutMergedCharges() {
		PaymentResponse payment = paymentService.create(createRequest(1L));

		PaymentTotalResponse total = paymentService.getTotal(payment.id());

		assertThat(total.mergedChargeAmount()).isEqualByComparingTo("0");
		assertThat(total.totalAmount()).isEqualByComparingTo("100000.00");
	}

	@Test
	@DisplayName("없는 결제의 합계를 조회하면 BusinessException 이 난다")
	void totalOfMissingThrows() {
		assertThatThrownBy(() -> paymentService.getTotal(99999L))
				.isInstanceOf(BusinessException.class)
				.hasMessage("결제 내역을 찾을 수 없습니다.");
	}

	/** 예약 1번에 대한 테스트용 추가 요금 등록 요청. */
	private AdditionalChargeCreateRequest chargeRequest(String amount, Boolean isMerged, Long paymentId) {
		return new AdditionalChargeCreateRequest(
				1L, AdditionalChargeType.MINIBAR, "생수 2병", new BigDecimal(amount), isMerged, paymentId, null);
	}

	/** 테스트용 등록 요청. 채널(수수료율 10%)·수단·금액은 테스트마다 중요하지 않아 고정값으로 둡니다. */
	private PaymentCreateRequest createRequest(Long reservationId) {
		Long channelId = ChannelFixture.save(channelRepository, "10.00", true).getId();
		return new PaymentCreateRequest(reservationId, channelId, "사전 결제", new BigDecimal("100000.00"), null);
	}

	/** 채널·금액만 다른 테스트용 등록 요청. */
	private PaymentCreateRequest paymentRequest(Long channelId, String amount) {
		return new PaymentCreateRequest(1L, channelId, "사전 결제", new BigDecimal(amount), null);
	}
}
