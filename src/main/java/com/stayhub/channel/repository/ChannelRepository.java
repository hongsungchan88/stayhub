package com.stayhub.channel.repository;

import com.stayhub.channel.entity.Channel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChannelRepository extends JpaRepository<Channel, Long> {
}
