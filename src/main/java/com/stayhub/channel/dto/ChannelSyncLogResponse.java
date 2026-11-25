package com.stayhub.channel.dto;

import com.stayhub.channel.entity.ChannelSyncLog;
import com.stayhub.channel.entity.ChannelSyncStatus;
import java.time.LocalDateTime;

/** 관리자 동기화 로그 화면에 표시하는 한 행입니다. */
public record ChannelSyncLogResponse(
        String channelName,
        String syncEvent,
        ChannelSyncStatus syncStatus,
        String errorMessage,
        String priorityRule,
        LocalDateTime syncedAt
) {
    /** 로그에서 화면에 표시할 항목을 꺼냅니다. */
    public static ChannelSyncLogResponse from(ChannelSyncLog log) {
        return new ChannelSyncLogResponse(
                log.getChannelName(), log.getSyncEvent(), log.getSyncStatus(),
                log.getErrorMessage(), log.getPriorityRule(), log.getSyncedAt());
    }
}