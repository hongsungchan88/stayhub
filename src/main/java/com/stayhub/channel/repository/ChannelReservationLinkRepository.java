package com.stayhub.channel.repository;

import com.stayhub.channel.entity.ChannelReservationLink;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChannelReservationLinkRepository
        extends JpaRepository<ChannelReservationLink, Long> {
    Optional<ChannelReservationLink> findByChannelIdAndExternalReservationId(
            Long channelId, String externalReservationId);
}