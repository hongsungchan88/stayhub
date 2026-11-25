package com.stayhub.channel.entity;

import com.stayhub.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "channel_reservation_links", uniqueConstraints = {
        @UniqueConstraint(name = "uk_chn_external_booking",
                columnNames = {"channel_id", "external_reservation_id"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChannelReservationLink extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "channel_reservation_link_id")
    private Long id;

    @Column(name = "channel_id", nullable = false)
    private Long channelId;

    @Column(name = "external_reservation_id", nullable = false, length = 100)
    private String externalReservationId;

    @Column(name = "reservation_id", nullable = false, unique = true)
    private Long reservationId;

    @Column(name = "applied_revision", nullable = false)
    private int appliedRevision;

    @Column(name = "external_confirmed_at", nullable = false)
    private Instant externalConfirmedAt;

    public static ChannelReservationLink create(Long channelId, String externalId,
                                                Long reservationId, int revision, Instant confirmedAt) {
        ChannelReservationLink link = new ChannelReservationLink();
        link.channelId = channelId;
        link.externalReservationId = externalId;
        link.reservationId = reservationId;
        link.appliedRevision = revision;
        link.externalConfirmedAt = confirmedAt;
        return link;
    }
}