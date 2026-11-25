package com.stayhub.channel.repository;

import com.stayhub.channel.entity.Channel;
import org.springframework.data.jpa.repository.JpaRepository;

/** 채널 정보를 조회하고 저장합니다. */
public interface ChannelRepository extends JpaRepository<Channel, Long> {
}