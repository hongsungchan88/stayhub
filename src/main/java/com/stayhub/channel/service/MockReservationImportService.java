package com.stayhub.channel.service;

import com.stayhub.channel.dto.MockChannelFeed;
import com.stayhub.channel.entity.Channel;
import com.stayhub.channel.entity.ChannelReservationLink;
import com.stayhub.channel.repository.ChannelRepository;
import com.stayhub.channel.repository.ChannelReservationLinkRepository;
import com.stayhub.common.BusinessException;
import com.stayhub.reservation.dto.ReservationCreateRequest;
import com.stayhub.reservation.service.ReservationService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.validation.Validator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MockReservationImportService {

    private static final String SOURCE = "MOCK_A";
    private final ChannelRepository channels;
    private final ChannelReservationLinkRepository links;
    private final ReservationService reservations;
    private final EntityManager em;
    private final Validator validator;

    public record Result(String outcome, int createdCount, String externalReservationId,
                         Long reservationId, int appliedRevision) {
    }

    // HTTP 응답은 컨트롤러에서 먼저 받는다. 잠금 중에는 DB 작업만 한다.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Result importOne(String externalId, List<MockChannelFeed.Booking> feed) {
        if (externalId == null || externalId.isBlank() || externalId.length() > 100)
            throw new BusinessException("외부 예약번호를 확인해 주세요.");

        var matching = feed.stream().filter(b -> b != null
                && SOURCE.equals(b.channelCode())
                && externalId.equals(b.externalReservationId())).toList();
        if (matching.size() != 1)
            throw new BusinessException("채널 A 응답에서 해당 외부 예약이 정확히 한 건이어야 합니다.");
        var booking = matching.getFirst();
        if (booking.revision() == null || booking.revision() < 1)
            throw new BusinessException("외부 예약 버전은 1 이상이어야 합니다.");

        Channel channel = lockSource();
        var existing = links.findByChannelIdAndExternalReservationId(channel.getId(), externalId);
        if (existing.isPresent()) {
            var link = existing.get();
            if (booking.revision() < link.getAppliedRevision())
                return result("STALE", 0, link);
            if (booking.revision() == link.getAppliedRevision()
                    && "CONFIRMED".equals(booking.status()))
                return result("ALREADY_IMPORTED", 0, link);
            // 다음 단계에서 기존 예약을 변경/취소할 자리. 처리하지 않은 버전을 기록하지 않는다.
            throw new BusinessException("외부 예약이 변경됐습니다. 이 최초 등록 기능에서는 변경·취소를 반영하지 않습니다.");
        }

        if (!"CONFIRMED".equals(booking.status()) || booking.confirmedAt() == null)
            throw new BusinessException("확정 시각이 있는 확정 예약만 새로 등록할 수 있습니다.");
        if (booking.roomNumber() == null || booking.roomNumber().isBlank())
            throw new BusinessException("외부 예약에 객실 번호가 없습니다.");

        Long roomId = em.createQuery("select r.id from Room r where r.roomNumber = :number", Long.class)
                .setParameter("number", booking.roomNumber().strip()).getResultList().stream()
                .findFirst().orElseThrow(() -> new BusinessException("StayHub에 해당 객실 번호가 없습니다."));

        var request = new ReservationCreateRequest(booking.guestName(), booking.guestPhone(),
                roomId, channel.getId(), booking.checkInDate(), booking.checkOutDate(), booking.guestCount());
        var errors = validator.validate(request);
        if (!errors.isEmpty())
            throw new BusinessException(errors.iterator().next().getMessage());

        // 객실 잠금·날짜 겹침·객실 정원은 기존 예약 서비스를 그대로 거친다.
        var reservation = reservations.create(request);
        var link = links.saveAndFlush(ChannelReservationLink.create(channel.getId(), externalId,
                reservation.id(), booking.revision(), booking.confirmedAt().toInstant()));
        return result("CREATED", 1, link);
    }

    private Channel lockSource() {
        var found = channels.findAllByChannelNameAndRegisteredTrue(SOURCE);
        if (found.size() != 1)
            throw new BusinessException("등록 상태인 MOCK_A 채널이 정확히 한 개 있어야 합니다.");
        var channel = found.getFirst();
        // 두 요청이 동시에 들어와도 이 채널의 등록을 차례로 처리한다.
        em.refresh(channel, LockModeType.PESSIMISTIC_WRITE);
        if (!channel.isRegistered() || !SOURCE.equals(channel.getChannelName()))
            throw new BusinessException("채널 설정이 변경됐습니다. 설정을 확인해 주세요.");
        return channel;
    }

    private Result result(String outcome, int count, ChannelReservationLink link) {
        return new Result(outcome, count, link.getExternalReservationId(),
                link.getReservationId(), link.getAppliedRevision());
    }
}
