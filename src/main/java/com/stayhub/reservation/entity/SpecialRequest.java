package com.stayhub.reservation.entity;

import com.stayhub.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

/** 예약에 연결된 특별 요청사항. */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "special_requests")
public class SpecialRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Long id;

    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;

    @Column(name = "request_content", nullable = false, columnDefinition = "TEXT")
    private String requestContent;

    @Enumerated(EnumType.STRING)
    @ColumnDefault("'RECEIVED'")
    @Column(name = "status", nullable = false, columnDefinition = "VARCHAR(20)")
    private SpecialRequestStatus status = SpecialRequestStatus.RECEIVED;

    @Column(name = "reject_reason", length = 255)
    private String rejectReason;

    @Column(name = "assigned_staff_id")
    private Long assignedStaffId;

    /** 새 요청은 접수 상태로 생성합니다. */
    public static SpecialRequest create(
            Long reservationId,
            String requestContent
    ) {
        SpecialRequest request = new SpecialRequest();
        request.reservationId = reservationId;
        request.requestContent = requestContent;
        return request;
    }

    /**
     * 처리 상태·거절 사유·담당자를 변경합니다.
     * 입력값과 담당자 존재 여부는 서비스에서 검증합니다.
     */
    public void changeProcessing(
            SpecialRequestStatus status,
            String rejectReason,
            Long assignedStaffId
    ) {
        this.status = status;
        this.rejectReason = rejectReason;
        this.assignedStaffId = assignedStaffId;
    }
}