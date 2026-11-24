package com.stayhub.channel.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record MockChannelFeed(
        boolean success,
        List<Booking> data,
        String message
) {
    public record Booking(
            String channelCode,
            String externalReservationId,
            Integer revision,
            String status,
            String roomNumber,
            String guestName,
            String guestPhone,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            Integer guestCount,
            OffsetDateTime confirmedAt
    ) {}
}