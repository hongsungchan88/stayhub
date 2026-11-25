package com.stayhub.reservation.entity;

import com.stayhub.common.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "checkin_checkouts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CheckInOut extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "checkin_id")
    private Long id;
    @Column(name = "reservation_id", nullable = false, unique = true)
    private Long reservationId;
    @Column(name = "key_code", length = 50)
    private String keyCode;
    @Column(name = "extra_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal extraFee = BigDecimal.ZERO;
    @Column(name = "is_settled", nullable = false)
    private boolean settled;
    @Column(name = "actual_check_in")
    private LocalDateTime actualCheckIn;
    @Column(name = "actual_check_out")
    private LocalDateTime actualCheckOut;

    public static CheckInOut start(Long reservationId, String keyCode, LocalDateTime now) {
        CheckInOut stay = new CheckInOut();
        stay.reservationId = reservationId;
        stay.keyCode = keyCode;
        stay.actualCheckIn = now;
        return stay;
    }

    public void finish(BigDecimal total, LocalDateTime now) {
        this.extraFee = total;
        this.settled = true;
        this.actualCheckOut = now;
    }
}