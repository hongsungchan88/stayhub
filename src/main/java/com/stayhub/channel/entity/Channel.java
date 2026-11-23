package com.stayhub.channel.entity;

import com.stayhub.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "channels")
public class Channel extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "channel_id")
    private Long id;

    @Column(name = "channel_name", nullable = false, length = 50)
    private String channelName;

    @ColumnDefault("0")
    @Column(name = "commission_rate", nullable = false,
            precision = 5, scale = 2)
    private BigDecimal commissionRate = new BigDecimal("0.00");

    @ColumnDefault("true")
    @Column(name = "is_registered", nullable = false)
    private boolean registered = true;

    @ColumnDefault("10")
    @Column(name = "default_commission_rate", nullable = false,
            precision = 5, scale = 2)
    private BigDecimal defaultCommissionRate = new BigDecimal("10.00");

    /** 수수료율과 기본 수수료율을 바꿉니다 (PAY-003, 채널 수수료 설정 SCR-PAY-03). 값 검사는 요청 DTO 에서 합니다. */
    public void changeCommissionRates(BigDecimal commissionRate, BigDecimal defaultCommissionRate) {
        this.commissionRate = commissionRate;
        this.defaultCommissionRate = defaultCommissionRate;
    }
}
