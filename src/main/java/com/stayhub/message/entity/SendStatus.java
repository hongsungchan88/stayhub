package com.stayhub.message.entity;

/** 답장(OUT) 발송 결과. 1차는 콘솔 발송이라 항상 SUCCESS, FAILED·재전송은 v2. */
public enum SendStatus {
    SUCCESS,
    FAILED
}
