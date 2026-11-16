package com.stayhub.message.dto;

/** 답장 요청. 공백·1,000자 초과 검사는 서비스에서 합니다. */
public record MessageReplyRequest(String content) {
}
