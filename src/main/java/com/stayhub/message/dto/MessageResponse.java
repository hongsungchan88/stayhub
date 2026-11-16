package com.stayhub.message.dto;

import com.stayhub.message.entity.Message;
import com.stayhub.message.entity.MessageDirection;
import com.stayhub.message.entity.SendStatus;
import java.time.LocalDateTime;

public record MessageResponse(
        Long messageId,
        MessageDirection direction,
        String content,
        SendStatus sendStatus,
        Long sentBy,
        LocalDateTime createdAt
) {

    public static MessageResponse from(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getDirection(),
                message.getContent(),
                message.getSendStatus(),
                message.getSentBy(),
                message.getCreatedAt()
        );
    }
}
