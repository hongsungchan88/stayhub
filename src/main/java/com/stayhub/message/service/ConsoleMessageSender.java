package com.stayhub.message.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** 1차 발송: 실제로 보내지 않고 서버 로그에 남깁니다(이메일 발송 정책 41행과 같은 방식). */
@Component
public class ConsoleMessageSender implements MessageSender {

    private static final Logger log = LoggerFactory.getLogger(ConsoleMessageSender.class);

    @Override
    public void send(String channelName, String guestName, String content) {
        log.info("[{}] {} ← {}", channelName, guestName, content);
    }
}
