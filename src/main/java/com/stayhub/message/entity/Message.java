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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

/** 스레드 안의 메시지 한 건. 삭제 기능 없음(유형 ③). */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "messages")
public class Message extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long id;

    @Column(name = "thread_id", nullable = false)
    private Long threadId;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, columnDefinition = "VARCHAR(10)")
    private MessageDirection direction;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "send_status", columnDefinition = "VARCHAR(20)")
    private SendStatus sendStatus;

    // 재전송(v2)에서 증가. 1차는 항상 0.
    @ColumnDefault("0")
    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "sent_by")
    private Long sentBy;

    /** 게스트가 보낸 메시지. 발송 상태·발신 직원이 없습니다. */
    public static Message incoming(Long threadId, String content) {
        Message message = new Message();
        message.threadId = threadId;
        message.direction = MessageDirection.IN;
        message.content = content;
        return message;
    }

    /** 직원 답장. 발송이 끝난 뒤 결과와 함께 만듭니다. */
    public static Message outgoing(Long threadId, String content, Long sentBy, SendStatus sendStatus) {
        Message message = new Message();
        message.threadId = threadId;
        message.direction = MessageDirection.OUT;
        message.content = content;
        message.sentBy = sentBy;
        message.sendStatus = sendStatus;
        return message;
    }
}
