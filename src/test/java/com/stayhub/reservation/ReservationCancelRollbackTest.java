package com.stayhub.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;

import com.stayhub.common.BusinessException;
import com.stayhub.common.PropertyScope;
import com.stayhub.payment.dto.PaymentCreateRequest;
import com.stayhub.payment.service.PaymentService;
import com.stayhub.reservation.service.ReservationService;
import com.stayhub.room.entity.Room;
import com.stayhub.room.repository.RoomRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 서비스 트랜잭션이 실제로 롤백되는지 DB를 다시 조회해 확인합니다.
 * 이 클래스에는 테스트용 @Transactional을 붙이지 않습니다.
 */
@SpringBootTest
class ReservationCancelRollbackTest {

    @Autowired
    private ReservationService reservationService;

    @MockitoSpyBean
    private PaymentService paymentService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("환불 반영 후 오류가 나면 예약 취소와 결제 환불이 모두 롤백된다")
    void refundFailureRollsBackCancellation() {
        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);

        Fixture data = createFixture(transaction);

        try {
            doAnswer(invocation -> {
                // 실제 환불 계산과 결제 상태 변경을 먼저 실행합니다.
                Object result = invocation.callRealMethod();

                // 예약과 결제의 변경을 DB에 보낸 뒤 오류를 발생시킵니다.
                entityManager.flush();

                assertThat(jdbcTemplate.queryForObject(
                        "SELECT status FROM reservations WHERE reservation_id = ?",
                        String.class,
                        data.reservationId()
                )).isEqualTo("CANCELLED");

                assertThat(jdbcTemplate.queryForObject(
                        "SELECT payment_status FROM payments WHERE payment_id = ?",
                        String.class,
                        data.paymentId()
                )).isEqualTo("REFUNDED");

                throw new BusinessException("테스트용 환불 실패");
            }).when(paymentService)
                    .refundForCancellation(data.reservationId());

            assertThatThrownBy(() -> reservationService.cancel(
                    data.reservationId(), "롤백 확인"
            ))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("테스트용 환불 실패");

            // 실패한 서비스 트랜잭션이 끝난 뒤 DB를 다시 확인합니다.
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT status FROM reservations WHERE reservation_id = ?",
                    String.class,
                    data.reservationId()
            )).isEqualTo("CONFIRMED");

            assertThat(jdbcTemplate.queryForObject(
                    "SELECT cancel_reason FROM reservations WHERE reservation_id = ?",
                    String.class,
                    data.reservationId()
            )).isNull();

            assertThat(jdbcTemplate.queryForObject(
                    "SELECT payment_status FROM payments WHERE payment_id = ?",
                    String.class,
                    data.paymentId()
            )).isEqualTo("COMPLETED");

            assertThat(jdbcTemplate.queryForObject(
                    "SELECT refund_amount FROM payments WHERE payment_id = ?",
                    BigDecimal.class,
                    data.paymentId()
            )).isNull();

            assertThat(jdbcTemplate.queryForObject(
                    "SELECT refund_request_date FROM payments WHERE payment_id = ?",
                    java.sql.Date.class,
                    data.paymentId()
            )).isNull();
        } finally {
            // 이 테스트에서 만든 데이터만 정리합니다.
            transaction.executeWithoutResult(status -> {
                jdbcTemplate.update(
                        "DELETE FROM payments WHERE payment_id = ?",
                        data.paymentId());
                jdbcTemplate.update(
                        "DELETE FROM reservations WHERE reservation_id = ?",
                        data.reservationId());
                jdbcTemplate.update(
                        "DELETE FROM rooms WHERE room_id = ?",
                        data.roomId());
                jdbcTemplate.update(
                        "DELETE FROM channels WHERE channel_id = ?",
                        data.channelId());
            });
        }
    }

    private Fixture createFixture(TransactionTemplate transaction) {
        return transaction.execute(status -> {
            String tag = UUID.randomUUID().toString()
                    .replace("-", "").substring(0, 12);
            String channelName = "RB" + tag;

            jdbcTemplate.update("""
                    INSERT INTO channels (
                        channel_name, commission_rate, default_commission_rate,
                        is_registered, created_at, updated_at
                    ) VALUES (?, 10.00, 10.00, TRUE, NOW(), NOW())
                    """, channelName);

            Long channelId = jdbcTemplate.queryForObject(
                    "SELECT channel_id FROM channels WHERE channel_name = ?",
                    Long.class,
                    channelName
            );

            Long roomId = roomRepository.save(Room.create(
                    PropertyScope.DEFAULT_PROPERTY_ID,
                    "RB" + tag,
                    "STANDARD",
                    new BigDecimal("100000.00"),
                    2
            )).getId();

            LocalDate checkIn = LocalDate.now().plusDays(10);

            Long reservationId = reservationService.create(
                    null, "롤백테스트", "010-0000-0000",
                    roomId, null,
                    checkIn, checkIn.plusDays(1), 1
            ).getId();

            Long paymentId = paymentService.create(
                    new PaymentCreateRequest(
                            reservationId,
                            channelId,
                            "사전 결제",
                            new BigDecimal("100000.00"),
                            null
                    )
            ).id();

            return new Fixture(
                    reservationId, paymentId, roomId, channelId);
        });
    }

    private record Fixture(
            Long reservationId,
            Long paymentId,
            Long roomId,
            Long channelId
    ) {
    }
}