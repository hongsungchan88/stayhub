package com.stayhub.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stayhub.common.BusinessException;
import com.stayhub.common.PropertyScope;
import com.stayhub.payment.dto.PaymentCreateRequest;
import com.stayhub.payment.entity.PaymentRefundRule;
import com.stayhub.payment.entity.PaymentStatus;
import com.stayhub.payment.service.PaymentService;
import com.stayhub.reservation.dto.ReservationUpdateRequest;
import com.stayhub.reservation.entity.Reservation;
import com.stayhub.reservation.entity.ReservationStatus;
import com.stayhub.reservation.repository.ReservationRepository;
import com.stayhub.reservation.service.ReservationService;
import com.stayhub.room.entity.Room;
import com.stayhub.room.repository.RoomRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** 예약 취소·환불 연결·날짜 제한·수동 노쇼 테스트. */
@SpringBootTest
@Transactional
class ReservationCancelNoShowTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private LocalDate today;
    private Long roomId;
    private Long channelId;
    private String tag;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();
        tag = UUID.randomUUID().toString()
                .replace("-", "").substring(0, 12);

        roomId = roomRepository.save(Room.create(
                PropertyScope.DEFAULT_PROPERTY_ID,
                "CN" + tag,
                "STANDARD",
                new BigDecimal("100000.00"),
                2
        )).getId();

        String channelName = "CN" + tag;

        jdbcTemplate.update("""
                INSERT INTO channels (
                    channel_name, commission_rate, default_commission_rate,
                    is_registered, created_at, updated_at
                ) VALUES (?, 10.00, 10.00, TRUE, NOW(), NOW())
                """, channelName);

        channelId = jdbcTemplate.queryForObject(
                "SELECT channel_id FROM channels WHERE channel_name = ?",
                Long.class,
                channelName
        );
    }

    @Test
    @DisplayName("정확히 체크인 3일 전에 취소하면 전액 환불하고 사유를 저장한다")
    void cancelThreeDaysBeforeFullRefund() {
        Long id = createReservation(today.plusDays(3));
        Long paymentId = pay(id, "100000.00");

        var preview = paymentService.previewCancelRefund(id);

        var result = reservationService.cancel(id, "  일정 변경  ");

        assertThat(result.status()).isEqualTo(ReservationStatus.CANCELLED);

        var payment = paymentService.getDetail(paymentId);
        assertThat(payment.paymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(payment.refundRuleApplied()).isEqualTo(PaymentRefundRule.FULL);
        assertThat(payment.refundAmount()).isEqualByComparingTo("100000.00");
        assertThat(payment.refundAmount())
                .isEqualByComparingTo(preview.refundTotal());
        assertThat(payment.refundRequestDate()).isEqualTo(today);

        flushAndClear();

        Reservation saved = reservationRepository.findById(id).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(saved.getCancelReason()).isEqualTo("일정 변경");

        // 취소된 예약은 겹침 검사에서 제외되어 같은 날짜에 다시 예약할 수 있습니다.
        assertThat(createReservation(today.plusDays(3))).isNotNull();
    }

    @Test
    @DisplayName("체크인 전날 취소하면 결제 완료 2건을 각각 50% 환불한다")
    void cancelTomorrowHalfRefund() {
        Long id = createReservation(today.plusDays(1));
        Long firstPaymentId = pay(id, "100000.00");
        Long secondPaymentId = pay(id, "30001.00");

        var preview = paymentService.previewCancelRefund(id);

        reservationService.cancel(id, null);

        var first = paymentService.getDetail(firstPaymentId);
        var second = paymentService.getDetail(secondPaymentId);

        assertThat(first.paymentStatus())
                .isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
        assertThat(second.paymentStatus())
                .isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
        assertThat(first.refundRuleApplied())
                .isEqualTo(PaymentRefundRule.PARTIAL);
        assertThat(first.refundAmount()).isEqualByComparingTo("50000.00");
        assertThat(second.refundAmount()).isEqualByComparingTo("15000.50");
        assertThat(first.refundAmount().add(second.refundAmount()))
                .isEqualByComparingTo(preview.refundTotal());

        flushAndClear();

        assertThat(reservationRepository.findById(id).orElseThrow()
                .getCancelReason()).isNull();
    }

    @Test
    @DisplayName("결제가 없고 취소 사유가 공백이어도 취소할 수 있다")
    void cancelWithoutPayment() {
        Long id = createReservation(today.plusDays(5));

        reservationService.cancel(id, "   ");
        flushAndClear();

        Reservation saved = reservationRepository.findById(id).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(saved.getCancelReason()).isNull();
    }

    @Test
    @DisplayName("당일·과거 예약은 취소와 날짜 변경을 거절한다")
    void rejectCancellationAndChangeOnOrAfterCheckIn() {
        for (LocalDate checkIn : new LocalDate[]{
                today, today.minusDays(1)
        }) {
            Long id = fixtureReservation(checkIn);

            assertThatThrownBy(() -> reservationService.cancel(id, "취소"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("체크인 당일 또는 지난 예약은 취소할 수 없습니다.");

            assertThatThrownBy(() -> reservationService.update(
                    id,
                    new ReservationUpdateRequest(
                            roomId, today.plusDays(10), today.plusDays(11)
                    )
            ))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("체크인 당일 또는 지난 예약은 변경할 수 없습니다.");

            Reservation unchanged =
                    reservationRepository.findById(id).orElseThrow();

            assertThat(unchanged.getStatus())
                    .isEqualTo(ReservationStatus.CONFIRMED);
            assertThat(unchanged.getCheckInDate()).isEqualTo(checkIn);
            assertThat(unchanged.getCancelReason()).isNull();
        }
    }

    @Test
    @DisplayName("과거 날짜의 신규 등록과 과거 날짜로의 변경을 거절한다")
    void rejectPastDates() {
        assertThatThrownBy(() ->
                createReservation(today.minusDays(1)))
                .isInstanceOf(BusinessException.class)
                .hasMessage(
                        "checkInDate: 체크인 날짜는 오늘 또는 이후여야 합니다.");

        Long id = createReservation(today.plusDays(5));

        assertThatThrownBy(() -> reservationService.update(
                id,
                new ReservationUpdateRequest(
                        roomId, today.minusDays(1), today.plusDays(1)
                )
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage(
                        "checkInDate: 변경할 체크인 날짜는 오늘 또는 이후여야 합니다.");
    }

    @Test
    @DisplayName("미래 예약을 오늘 체크인으로 당길 수 있다")
    void moveFutureReservationToToday() {
        Long id = createReservation(today.plusDays(5));

        var result = reservationService.update(
                id,
                new ReservationUpdateRequest(
                        roomId, today, today.plusDays(1)
                )
        );

        assertThat(result.checkInDate()).isEqualTo(today);
        assertThat(result.checkOutDate()).isEqualTo(today.plusDays(1));
        assertThat(result.status()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    @DisplayName("오늘 예약을 노쇼 처리해도 결제는 유지하고 같은 날짜 재예약이 가능하다")
    void noShowKeepsPaymentAndReleasesDates() {
        Long id = createReservation(today);
        Long paymentId = pay(id, "100000.00");

        reservationService.markNoShow(id);
        flushAndClear();

        assertThat(reservationRepository.findById(id).orElseThrow()
                .getStatus()).isEqualTo(ReservationStatus.NOSHOW);

        var payment = paymentService.getDetail(paymentId);
        assertThat(payment.paymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.refundRequestDate()).isNull();
        assertThat(payment.refundAmount()).isNull();

        assertThat(createReservation(today)).isNotNull();
    }

    @Test
    @DisplayName("미래 예약은 노쇼 처리할 수 없다")
    void futureReservationCannotBeNoShow() {
        Long id = createReservation(today.plusDays(1));

        assertThatThrownBy(() -> reservationService.markNoShow(id))
                .isInstanceOf(BusinessException.class)
                .hasMessage(
                        "체크인 예정일이 오늘이거나 지난 예약만 노쇼 처리할 수 있습니다.");

        assertThat(reservationRepository.findById(id).orElseThrow()
                .getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    @DisplayName("255자를 넘는 취소 사유는 거절한다")
    void rejectLongCancellationReason() {
        Long id = createReservation(today.plusDays(5));

        assertThatThrownBy(() ->
                reservationService.cancel(id, "가".repeat(256)))
                .isInstanceOf(BusinessException.class)
                .hasMessage(
                        "cancelReason: 취소 사유는 255자 이내로 입력해 주세요.");

        assertThat(reservationRepository.findById(id).orElseThrow()
                .getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    private Long createReservation(LocalDate checkIn) {
        return reservationService.create(
                null, "예약테스트" + tag, "010-0000-0000",
                roomId, null,
                checkIn, checkIn.plusDays(1), 1
        ).getId();
    }

    /**
     * 이미 과거에 생성된 예약 상황을 재현하는 테스트 전용 데이터입니다.
     * 실제 등록 코드는 반드시 ReservationService.create를 사용합니다.
     */
    private Long fixtureReservation(LocalDate checkIn) {
        return reservationRepository.saveAndFlush(Reservation.create(
                "FIX-" + UUID.randomUUID(),
                null, "기존예약테스트", "010-0000-0000",
                roomId, null,
                checkIn, checkIn.plusDays(1), 1
        )).getId();
    }

    private Long pay(Long id, String amount) {
        return paymentService.create(new PaymentCreateRequest(
                id, channelId, "사전 결제",
                new BigDecimal(amount), null
        )).id();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}