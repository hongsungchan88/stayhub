package com.stayhub.message.entity;

/** 채널 메시지 수집 상태. 1차는 채널 연동이 없어 항상 SUCCESS, DELAYED("연동 지연")는 v2 수집 배치에서 씁니다. */
public enum SyncStatus {
    SUCCESS,
    DELAYED
}
