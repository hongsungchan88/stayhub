package com.stayhub.message.service;

/**
 * 채널로 답장을 보내는 곳. 1차는 {@link ConsoleMessageSender}(서버 로그 출력),
 * 채널 API 연동(v2)에서 구현체만 바꿉니다. 실패하면 예외를 던집니다.
 */
public interface MessageSender {

    void send(String channelName, String guestName, String content);
}
