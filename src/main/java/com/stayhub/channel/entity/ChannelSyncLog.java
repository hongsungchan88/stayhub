package com.stayhub.channel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

/** 모의 채널에서 예약 한 건을 가져오려고 시도한 결과를 기록합니다. */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "channel_sync_logs")
public class ChannelSyncLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sync_id")
    private Long id;

    @Column(name = "channel_name", nullable = false, length = 50)
    private String channelName;

    @Column(name = "ota_credential_info", nullable = false, length = 255)
    private String otaCredentialInfo;

    @Column(name = "sync_event", nullable = false, length = 50)
    private String syncEvent;

    @Enumerated(EnumType.STRING)
    @ColumnDefault("'SUCCESS'")
    @Column(name = "sync_status", nullable = false, columnDefinition = "VARCHAR(20)")
    private ChannelSyncStatus syncStatus;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "priority_rule", length = 100)
    private String priorityRule;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "synced_at", nullable = false, updatable = false,
            columnDefinition = "DATETIME")
    private LocalDateTime syncedAt;

    /** 모의 채널 A의 가져오기 결과를 만들고, 실패한 경우 이유도 남깁니다. */
    public static ChannelSyncLog forMockImport(
            String externalReservationId, ChannelSyncStatus status, String reason) {

        ChannelSyncLog log = new ChannelSyncLog();
        log.channelName = "모의 채널 A";
        log.otaCredentialInfo = "MOCK(앱 내 JSON)";
        log.syncEvent = importEvent(externalReservationId);
        log.syncStatus = Objects.requireNonNull(status);
        log.syncedAt = LocalDateTime.now();

        if (status != ChannelSyncStatus.SUCCESS) {
            log.errorMessage = reason == null || reason.isBlank()
                    ? "예약을 가져오지 못했습니다." : reason;
        }
        if (status == ChannelSyncStatus.CONFLICT) {
            log.priorityRule = "먼저 확정된 예약 우선";
        }
        return log;
    }

    private static String importEvent(String externalReservationId) {
        String bookingId = externalReservationId == null || externalReservationId.isBlank()
                ? "(예약번호 없음)" : externalReservationId.strip();
        String event = "IMPORT " + bookingId;

        // 잘못된 긴 예약번호를 받아도 50자 제한 때문에 실패 로그까지 사라지지 않게 합니다.
        if (event.codePointCount(0, event.length()) > 50) {
            return event.substring(0, event.offsetByCodePoints(0, 50));
        }
        return event;
    }
}