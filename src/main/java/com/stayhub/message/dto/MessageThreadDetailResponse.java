package com.stayhub.message.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 대화창: 스레드 정보와 메시지(오래된 것부터). reservationCode 는 예약이 연결된 경우만. */
public record MessageThreadDetailResponse(
        Long threadId,
        Long channelId,
        String channelName,
        String guestName,
        Long reservationId,
        String reservationCode,
        boolean isRead,
        LocalDateTime lastMessageAt,
        List<MessageResponse> messages
) {
}
