package com.stayhub.message.entity;

import com.stayhub.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

/** 채널·게스트별 대화 묶음(COM-001). 삭제 기능 없음(유형 ③). */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "message_threads")
public class MessageThread extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "thread_id")
    private Long id;

    @Column(name = "channel_id", nullable = false)
    private Long channelId;

    @Column(name = "reservation_id")
    private Long reservationId;

    @Column(name = "guest_name", length = 50)
    private String guestName;

    @Enumerated(EnumType.STRING)
    @ColumnDefault("'SUCCESS'")
    @Column(name = "sync_status", nullable = false, columnDefinition = "VARCHAR(20)")
    private SyncStatus syncStatus = SyncStatus.SUCCESS;

    @Column(name = "last_message_at", nullable = false)
    private LocalDateTime lastMessageAt;

    @ColumnDefault("false")
    @Column(name = "is_read", nullable = false)
    private boolean read;

    public static MessageThread create(
            Long channelId,
            String guestName,
            Long reservationId,
            LocalDateTime lastMessageAt
    ) {
        MessageThread thread = new MessageThread();
        thread.channelId = channelId;
        thread.guestName = guestName;
        thread.reservationId = reservationId;
        thread.lastMessageAt = lastMessageAt;
        return thread;
    }

    /** 게스트 메시지가 들어오면 다시 미읽음이 됩니다. */
    public void receive(LocalDateTime at) {
        this.lastMessageAt = at;
        this.read = false;
    }

    public void reply(LocalDateTime at) {
        this.lastMessageAt = at;
    }

    public void markRead() {
        this.read = true;
    }
}
