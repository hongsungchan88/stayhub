package com.stayhub.channel.repository;

import com.stayhub.channel.entity.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChannelRepository extends JpaRepository<Channel, Long> {
    List<Channel> findAllByChannelNameAndRegisteredTrue(String channelName);
}
