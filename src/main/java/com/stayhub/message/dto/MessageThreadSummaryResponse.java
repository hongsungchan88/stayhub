package com.stayhub.message.dto;

import java.time.LocalDateTime;

/** 인박스 목록 한 줄. needsReply: 마지막 메시지가 게스트(IN) 것이라 답장이 필요함(FLW-07 "수동 응대 필요"). */
public record MessageThreadSummaryResponse(
        Long threadId,
        Long channelId,
        String channelName,
        String guestName,
        Long reservationId,
        boolean isRead,
        LocalDateTime lastMessageAt,
        String lastMessagePreview,
        boolean needsReply
) {
}
