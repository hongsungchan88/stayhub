package com.stayhub.channel.repository;

import com.stayhub.channel.entity.ChannelSyncLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** 채널 동기화 로그를 저장하고 최신순으로 조회합니다. */
public interface ChannelSyncLogRepository extends JpaRepository<ChannelSyncLog, Long> {

    /** 기록 시각이 같으면 더 큰 로그 ID를 먼저 보여 줍니다. */
    Page<ChannelSyncLog> findAllByOrderBySyncedAtDescIdDesc(Pageable pageable);
}