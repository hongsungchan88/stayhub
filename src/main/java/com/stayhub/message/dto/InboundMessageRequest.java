package com.stayhub.message.dto;

/** 시연용 모의 수신 요청. */
public record InboundMessageRequest(Long channelId, String guestName, String content) {
}
