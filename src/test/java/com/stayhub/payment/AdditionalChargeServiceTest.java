package com.stayhub.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stayhub.channel.repository.ChannelRepository;
import com.stayhub.common.BusinessException;
import com.stayhub.common.PageResponse;
import com.stayhub.payment.dto.AdditionalChargeCreateRequest;
import com.stayhub.payment.dto.AdditionalChargeResponse;
import com.stayhub.payment.dto.AdditionalChargeUpdateRequest;
import com.stayhub.payment.dto.PaymentCreateRequest;
import com.stayhub.payment.entity.AdditionalChargeType;
import com.stayhub.payment.entity.PaymentStatus;
import com.stayhub.payment.repository.AdditionalChargeRepository;
import com.stayhub.payment.repository.PaymentRepository;
import com.stayhub.payment.service.AdditionalChargeService;
import com.stayhub.payment.service.PaymentService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

/**
 * 추가 요금 서비스 테스트.
 *
 * <p>실제 DB(application-local.yml 의 MySQL)에 붙어서 등록·조회·수정이 제대로 되는지 확인합니다.
 * {@code @Transactional} 이 붙어 있어서 테스트가 끝나면 넣었던 데이터가 전부 되돌려집니다(롤백).
 * 삭제 기능이 없는 테이블이라 삭제 테스트는 없습니다.
 */
@SpringBootTest
@Transactional
class AdditionalChargeServiceTest {

	@Autowired
	private AdditionalChargeService additionalChargeService;

	@Autowired
	private AdditionalChargeRepository additionalChargeRepository;

	@Autowired
	private PaymentService paymentService;

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private ChannelRepository channelRepository;

	@BeforeEach
	void setUp() {
		// 개발 DB 에 이미 들어 있는 데이터 때문에 결과가 흔들리지 않도록 테스트 안에서만 비웁니다.
		// @Transactional 이라 테스트가 끝나면 지운 것까지 원래대로 돌아옵니다.
		// 경고: @Transactional을 지우면 개발 DB의 이 테이블이 실제로 비워집니다. 지우지 마세요.
		additionalChargeRepository.deleteAll();
	}

	@Test
	@DisplayName("등록하면 목록에 나온다")
	void createThenAppearsInList() {
		AdditionalChargeResponse created = additionalChargeService.create(createRequest(AdditionalChargeType.MINIBAR));

		PageResponse<AdditionalChargeResponse> all = additionalChargeService.getList(0);
		assertThat(all.content()).extracting(AdditionalChargeResponse::id).containsExactly(created.id());

		// 비워 보낸 합산·정산 여부는 false, 청구 일시는 지금 시각으로 채워진다
		assertThat(created.isMerged()).isFalse();
		assertThat(created.isSettled()).isFalse();
		assertThat(created.chargedAt()).isNotNull();
	}

	@Test
	@DisplayName("수정하면 바뀐 값이 조회된다")
	void updateIsApplied() {
		AdditionalChargeResponse created = additionalChargeService.create(createRequest(AdditionalChargeType.MINIBAR));
		Long paymentId = createPayment(2L);

		additionalChargeService.update(created.id(), new AdditionalChargeUpdateRequest(
				2L, AdditionalChargeType.LATE_CHECKOUT, "2시간 연장", new BigDecimal("30000.00"), true, paymentId, true));

		AdditionalChargeResponse found = additionalChargeService.getDetail(created.id());
		assertThat(found.reservationId()).isEqualTo(2L);
		assertThat(found.chargeType()).isEqualTo(AdditionalChargeType.LATE_CHECKOUT);
		assertThat(found.description()).isEqualTo("2시간 연장");
		assertThat(found.amount()).isEqualByComparingTo("30000.00");
		assertThat(found.isMerged()).isTrue();
		assertThat(found.paymentId()).isEqualTo(paymentId);
		assertThat(found.isSettled()).isTrue();
	}

	@Test
	@DisplayName("없는 항목을 상세 조회하면 BusinessException 이 난다")
	void getMissingThrows() {
		assertThatThrownBy(() -> additionalChargeService.getDetail(99999L))
				.isInstanceOf(BusinessException.class)
				.hasMessage("추가 요금을 찾을 수 없습니다.");
	}

	@Test
	@DisplayName("합산인데 결제를 지정하지 않으면 BusinessException 이 난다")
	void mergeWithoutPaymentThrows() {
		assertThatThrownBy(() -> additionalChargeService.create(mergeRequest(1L, true, null)))
				.isInstanceOf(BusinessException.class)
				.hasMessage("합산할 결제를 선택해 주세요.");
	}

	@Test
	@DisplayName("합산하지 않는데 결제를 지정하면 BusinessException 이 난다")
	void paymentWithoutMergeThrows() {
		Long paymentId = createPayment(1L);

		assertThatThrownBy(() -> additionalChargeService.create(mergeRequest(1L, false, paymentId)))
				.isInstanceOf(BusinessException.class)
				.hasMessage("합산하지 않는 추가 요금에는 결제를 지정할 수 없습니다.");
	}

	@Test
	@DisplayName("없는 결제에 합산하면 BusinessException 이 난다")
	void mergeToMissingPaymentThrows() {
		assertThatThrownBy(() -> additionalChargeService.create(mergeRequest(1L, true, 99999L)))
				.isInstanceOf(BusinessException.class)
				.hasMessage("합산할 결제 내역을 찾을 수 없습니다.");
	}

	@Test
	@DisplayName("다른 예약의 결제에 합산하면 등록·수정 모두 BusinessException 이 난다")
	void mergeToOtherReservationThrows() {
		Long otherPaymentId = createPayment(2L);
		AdditionalChargeResponse created = additionalChargeService.create(createRequest(AdditionalChargeType.MINIBAR));

		assertThatThrownBy(() -> additionalChargeService.create(mergeRequest(1L, true, otherPaymentId)))
				.isInstanceOf(BusinessException.class)
				.hasMessage("같은 예약의 결제에만 합산할 수 있습니다.");
		assertThatThrownBy(() -> additionalChargeService.update(created.id(), new AdditionalChargeUpdateRequest(
				1L, AdditionalChargeType.MINIBAR, "생수 2병", new BigDecimal("4000.00"), true, otherPaymentId, null)))
				.isInstanceOf(BusinessException.class)
				.hasMessage("같은 예약의 결제에만 합산할 수 있습니다.");
	}

	@Test
	@DisplayName("결제 완료가 아닌 결제(FAILED·REFUND_REQUESTED)에는 합산할 수 없고, COMPLETED 에는 된다")
	void mergeOnlyToCompletedPayment() {
		Long paymentId = createPayment(1L);
		// FAILED 로 바꾸는 흐름이 아직 없어서 테스트에서 직접 넣습니다(TaxInvoiceServiceTest 와 같은 방식).
		ReflectionTestUtils.setField(paymentRepository.findById(paymentId).orElseThrow(), "paymentStatus", PaymentStatus.FAILED);

		assertThatThrownBy(() -> additionalChargeService.create(mergeRequest(1L, true, paymentId)))
				.isInstanceOf(BusinessException.class)
				.hasMessage("결제 완료 상태의 결제에만 합산할 수 있습니다.");

		Long refundingId = createPayment(1L);
		paymentRepository.findById(refundingId).orElseThrow().changeStatus(PaymentStatus.REFUND_REQUESTED);
		assertThatThrownBy(() -> additionalChargeService.create(mergeRequest(1L, true, refundingId)))
				.isInstanceOf(BusinessException.class)
				.hasMessage("결제 완료 상태의 결제에만 합산할 수 있습니다.");

		Long completedId = createPayment(1L);
		assertThat(additionalChargeService.create(mergeRequest(1L, true, completedId)).paymentId()).isEqualTo(completedId);
	}

	/** 테스트용 결제를 등록하고 ID 를 돌려줍니다. */
	private Long createPayment(Long reservationId) {
		Long channelId = ChannelFixture.save(channelRepository, "10.00", true).getId();
		return paymentService.create(new PaymentCreateRequest(
				reservationId, channelId, "사전 결제", new BigDecimal("100000.00"), null)).id();
	}

	/** 합산 정보만 다른 테스트용 등록 요청. */
	private AdditionalChargeCreateRequest mergeRequest(Long reservationId, Boolean isMerged, Long paymentId) {
		return new AdditionalChargeCreateRequest(
				reservationId, AdditionalChargeType.MINIBAR, "생수 2병", new BigDecimal("4000.00"), isMerged, paymentId, null);
	}

	/** 테스트용 등록 요청. 예약·금액은 고정값, 합산·정산 여부는 비워서 기본값을 씁니다. */
	private AdditionalChargeCreateRequest createRequest(AdditionalChargeType chargeType) {
		return new AdditionalChargeCreateRequest(
				1L, chargeType, "생수 2병", new BigDecimal("4000.00"), null, null, null);
	}
}
