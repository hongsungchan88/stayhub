package com.stayhub.channel.service;

import com.stayhub.channel.dto.MockChannelFeed;
import com.stayhub.channel.entity.Channel;
import com.stayhub.channel.entity.ChannelReservationLink;
import com.stayhub.channel.repository.ChannelRepository;
import com.stayhub.channel.repository.ChannelReservationLinkRepository;
import com.stayhub.common.BusinessException;
import com.stayhub.reservation.dto.ReservationCreateRequest;
import com.stayhub.reservation.service.ReservationService;
import jakarta.validation.Validator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.stayhub.room.repository.RoomRepository;
import com.stayhub.reservation.exception.ReservationOverlapException;
import java.time.LocalDate;
import com.stayhub.reservation.dto.ReservationResponse;
import jakarta.validation.ConstraintViolation;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MockReservationImportService {

    private static final String SOURCE = "MOCK_A";
    private static final Long MOCK_CHANNEL_A_ID = 3L;
    private final ChannelRepository channels;
    private final ChannelReservationLinkRepository links;
    private final ReservationService reservations;
    private final RoomRepository roomRepository;
    private final Validator validator;
    private final ChannelSyncLogService syncLogService;

    public record Result(String outcome, int createdCount, String externalReservationId,
                         Long reservationId, int appliedRevision) {
    }

    /** 모의 채널 A의 확정 예약 한 건을 가져옵니다. */
    @Transactional
    public Result importOne(String externalId, List<MockChannelFeed.Booking> feed) {
        Result result = applyImport(externalId, feed);
        syncLogService.recordSuccess(externalId);
        return result;
    }

    private Result applyImport(String externalId, List<MockChannelFeed.Booking> feed) {
        if (externalId == null || externalId.isBlank() || externalId.length() > 100)
            throw new BusinessException("외부 예약번호를 확인해 주세요.");

        List<MockChannelFeed.Booking> matching = feed.stream().filter(candidate -> candidate != null
                && SOURCE.equals(candidate.channelCode())
                && externalId.equals(candidate.externalReservationId())).toList();
        if (matching.size() != 1)
            throw new BusinessException("채널 A 응답에서 해당 외부 예약이 정확히 한 건이어야 합니다.");
        MockChannelFeed.Booking booking = matching.getFirst();
        if (booking.revision() == null || booking.revision() < 1)
            throw new BusinessException("외부 예약 버전은 1 이상이어야 합니다.");

        Channel channel = getRegisteredMockChannel();
        Optional<ChannelReservationLink> existing = links.findByChannelIdAndExternalReservationId(channel.getId(), externalId);
        if (existing.isPresent()) {
            ChannelReservationLink link = existing.get();
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

        rejectLaterConfirmation(booking, feed);
        Long roomId = roomRepository.findByRoomNumber(booking.roomNumber().strip())
                .orElseThrow(() -> new BusinessException("StayHub에 해당 객실 번호가 없습니다."))
                .getId();

        ReservationCreateRequest request = new ReservationCreateRequest(booking.guestName(), booking.guestPhone(),
                roomId, channel.getId(), booking.checkInDate(), booking.checkOutDate(), booking.guestCount());
        Set<ConstraintViolation<ReservationCreateRequest>> errors = validator.validate(request);
        if (!errors.isEmpty())
            throw new BusinessException(errors.iterator().next().getMessage());

        // 객실 잠금·날짜 겹침·객실 정원은 기존 예약 서비스를 그대로 거친다.
        ReservationResponse reservation = reservations.create(request);
        ChannelReservationLink link = links.saveAndFlush(ChannelReservationLink.create(channel.getId(), externalId,
                reservation.id(), booking.revision(), booking.confirmedAt().toInstant()));
        return result("CREATED", 1, link);
    }

    private void rejectLaterConfirmation(MockChannelFeed.Booking selected,
                                         List<MockChannelFeed.Booking> feed) {
        if (selected.checkInDate() == null || selected.checkOutDate() == null
                || !selected.checkOutDate().isAfter(selected.checkInDate())
                || selected.checkInDate().isBefore(LocalDate.now())) {
            return; // 잘못된 날짜는 기존 예약 검증에서 안내합니다.
        }

        for (MockChannelFeed.Booking other : feed) {
            if (other == null || !SOURCE.equals(other.channelCode())
                    || other.externalReservationId() == null
                    || other.externalReservationId().isBlank()
                    || selected.externalReservationId().equals(other.externalReservationId())
                    || !"CONFIRMED".equals(other.status()) || other.confirmedAt() == null
                    || other.roomNumber() == null
                    || !selected.roomNumber().strip().equals(other.roomNumber().strip())
                    || other.checkInDate() == null || other.checkOutDate() == null
                    || !other.checkOutDate().isAfter(other.checkInDate())) {
                continue;
            }

            boolean datesOverlap = selected.checkInDate().isBefore(other.checkOutDate())
                    && other.checkInDate().isBefore(selected.checkOutDate());

            if (datesOverlap && other.confirmedAt().isBefore(selected.confirmedAt())) {
                throw new ReservationOverlapException(
                        "같은 객실·날짜에 먼저 확정된 채널 예약이 있습니다.");
            }
        }
    }

    private Channel getRegisteredMockChannel() {
        return channels.findById(MOCK_CHANNEL_A_ID)
                .filter(Channel::isRegistered)
                .orElseThrow(() -> new BusinessException(
                        "모의 채널 A(3번)가 없거나 등록 상태가 아닙니다."));
    }

    private Result result(String outcome, int count, ChannelReservationLink link) {
        return new Result(outcome, count, link.getExternalReservationId(),
                link.getReservationId(), link.getAppliedRevision());
    }
}
