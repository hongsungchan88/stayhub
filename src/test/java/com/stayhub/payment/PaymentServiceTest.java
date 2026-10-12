package com.stayhub.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stayhub.common.BusinessException;
import com.stayhub.common.PageResponse;
import com.stayhub.payment.dto.PaymentCreateRequest;
import com.stayhub.payment.dto.PaymentResponse;
import com.stayhub.payment.dto.PaymentUpdateRequest;
import com.stayhub.payment.entity.PaymentStatus;
import com.stayhub.payment.repository.PaymentRepository;
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

		// 상태는 COMPLETED, 비워 보낸 수수료·실매출은 0, 환불 값은 비어 있다
		assertThat(created.paymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
		assertThat(created.commissionAmount()).isEqualByComparingTo("0");
		assertThat(created.netRevenue()).isEqualByComparingTo("0");
		assertThat(created.refundAmount()).isNull();
		assertThat(created.refundRuleApplied()).isNull();
	}

	@Test
	@DisplayName("수정하면 바뀐 값이 조회된다")
	void updateIsApplied() {
		PaymentResponse created = paymentService.create(createRequest(1L));

		paymentService.update(created.id(), new PaymentUpdateRequest(
				2L, 3L, "현장 결제", new BigDecimal("200000.00"),
				new BigDecimal("20000.00"), new BigDecimal("180000.00"), "중복 결제 확인 요청"));

		PaymentResponse found = paymentService.getDetail(created.id());
		assertThat(found.reservationId()).isEqualTo(2L);
		assertThat(found.channelId()).isEqualTo(3L);
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

	/** 테스트용 등록 요청. 채널·수단·금액은 테스트마다 중요하지 않아 고정값으로 두고, 수수료·실매출은 비웁니다. */
	private PaymentCreateRequest createRequest(Long reservationId) {
		return new PaymentCreateRequest(
				reservationId, 1L, "사전 결제", new BigDecimal("100000.00"), null, null, null);
	}
}
