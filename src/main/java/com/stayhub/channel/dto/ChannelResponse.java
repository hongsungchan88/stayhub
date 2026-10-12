package com.stayhub.channel.dto;

import com.stayhub.channel.entity.Channel;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ChannelResponse(
        Long id,
        String channelName,
        BigDecimal commissionRate,
        boolean isRegistered,
        BigDecimal defaultCommissionRate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ChannelResponse from(Channel channel) {
        return new ChannelResponse(
                channel.getId(),
                channel.getChannelName(),
                channel.getCommissionRate(),
                channel.isRegistered(),
                channel.getDefaultCommissionRate(),
                channel.getCreatedAt(),
                channel.getUpdatedAt()
        );
    }
}
