package com.stayhub.message.service;

import com.stayhub.channel.entity.Channel;
import com.stayhub.channel.repository.ChannelRepository;
import com.stayhub.common.BusinessException;
import com.stayhub.common.PageResponse;
import com.stayhub.message.dto.InboundMessageRequest;
import com.stayhub.message.dto.MessageReplyRequest;
import com.stayhub.message.dto.MessageResponse;
import com.stayhub.message.dto.MessageThreadDetailResponse;
import com.stayhub.message.dto.MessageThreadSummaryResponse;
import com.stayhub.message.entity.Message;
import com.stayhub.message.entity.MessageDirection;
import com.stayhub.message.entity.MessageThread;
import com.stayhub.message.entity.SendStatus;
import com.stayhub.message.repository.MessageRepository;
import com.stayhub.message.repository.MessageThreadRepository;
import com.stayhub.reservation.entity.Reservation;
import com.stayhub.reservation.entity.ReservationStatus;
import com.stayhub.reservation.repository.ReservationRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 통합 인박스(COM-001): 스레드 목록·대화 조회·읽음 처리·답장·모의 수신. */
@Service
@Transactional(readOnly = true)
public class MessageThreadService {

    public static final int REPLY_MAX_LENGTH = 1000;
    public static final int PREVIEW_LENGTH = 50;
    private static final int GUEST_NAME_MAX_LENGTH = 50;
    private static final List<ReservationStatus> MATCHABLE_STATUSES =
            List.of(ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_IN);

    private final MessageThreadRepository threadRepository;
    private final MessageRepository messageRepository;
    private final ChannelRepository channelRepository;
    private final ReservationRepository reservationRepository;
    private final MessageSender messageSender;

    public MessageThreadService(
            MessageThreadRepository threadRepository,
            MessageRepository messageRepository,
            ChannelRepository channelRepository,
            ReservationRepository reservationRepository,
            MessageSender messageSender
    ) {
        this.threadRepository = threadRepository;
        this.messageRepository = messageRepository;
        this.channelRepository = channelRepository;
        this.reservationRepository = reservationRepository;
        this.messageSender = messageSender;
    }

    /** 최근 메시지가 온 스레드부터 20건씩. channelId 가 있으면 그 채널만. */
    public PageResponse<MessageThreadSummaryResponse> getList(Long channelId, int page) {
        if (page < 0) {
            throw new BusinessException("페이지 번호는 0 이상이어야 합니다.");
        }

        PageRequest pageable = PageRequest.of(
                page,
                PageResponse.DEFAULT_PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "lastMessageAt", "id")
        );

        Page<MessageThread> threads;
        if (channelId == null) {
            threads = threadRepository.findAll(pageable);
        } else {
            findChannel(channelId);
            threads = threadRepository.findByChannelId(channelId, pageable);
        }

        // 채널 수가 적어 전부 읽어 이름표로 씁니다.
        Map<Long, String> channelNames = channelRepository.findAll().stream()
                .collect(Collectors.toMap(Channel::getId, Channel::getChannelName));
        Map<Long, Message> lastMessages = threads.isEmpty()
                ? Map.of()
                : messageRepository.findLastMessages(threads.map(MessageThread::getId).toList()).stream()
                        .collect(Collectors.toMap(Message::getThreadId, Function.identity()));

        return PageResponse.from(threads.map(thread -> {
            Message last = lastMessages.get(thread.getId());
            return new MessageThreadSummaryResponse(
                    thread.getId(),
                    thread.getChannelId(),
                    channelNames.get(thread.getChannelId()),
                    thread.getGuestName(),
                    thread.getReservationId(),
                    thread.isRead(),
                    thread.getLastMessageAt(),
                    last == null ? null : preview(last.getContent()),
                    last != null && last.getDirection() == MessageDirection.IN
            );
        }));
    }

    /** 스레드 정보와 메시지 전체(오래된 것부터). 읽음 처리는 하지 않습니다. */
    public MessageThreadDetailResponse getDetail(Long threadId) {
        MessageThread thread = findThread(threadId);
        String reservationCode = thread.getReservationId() == null
                ? null
                : reservationRepository.findById(thread.getReservationId())
                        .map(Reservation::getReservationCode)
                        .orElse(null);

        return new MessageThreadDetailResponse(
                thread.getId(),
                thread.getChannelId(),
                channelRepository.findById(thread.getChannelId()).map(Channel::getChannelName).orElse(null),
                thread.getGuestName(),
                thread.getReservationId(),
                reservationCode,
                thread.isRead(),
                thread.getLastMessageAt(),
                messageRepository.findByThreadIdOrderByCreatedAtAscIdAsc(thread.getId()).stream()
                        .map(MessageResponse::from)
                        .toList()
        );
    }

    @Transactional
    public void markRead(Long threadId) {
        findThread(threadId).markRead();
    }

    /** 답장을 채널로 보내고 OUT 메시지로 저장합니다. sentBy: 로그인한 직원. */
    @Transactional
    public MessageResponse reply(Long threadId, MessageReplyRequest request, Long sentBy) {
        MessageThread thread = findThread(threadId);
        String content = request.content() == null ? "" : request.content().trim();

        if (content.isEmpty()) {
            throw new BusinessException("content: 답장 내용을 입력해 주세요.");
        }
        if (content.length() > REPLY_MAX_LENGTH) {
            throw new BusinessException("content: 답장은 1,000자 이내로 입력해 주세요.");
        }

        Channel channel = findChannel(thread.getChannelId());
        // 1차는 콘솔 발송이라 실패가 없습니다. FAILED·재전송·retry_count 증가는 v2.
        messageSender.send(channel.getChannelName(), thread.getGuestName(), content);

        Message message = messageRepository.saveAndFlush(
                Message.outgoing(thread.getId(), content, sentBy, SendStatus.SUCCESS));
        thread.reply(LocalDateTime.now());

        return MessageResponse.from(message);
    }

    /**
     * 게스트 메시지 모의 수신.
     * 같은 채널·게스트 스레드가 있으면 거기에, 없으면 새 스레드를 만들고
     * 게스트 이름이 같은 진행 중 예약(확정·체크인)이 정확히 1건이면 연결합니다.
     */
    @Transactional
    public MessageThreadDetailResponse receive(InboundMessageRequest request) {
        if (request.channelId() == null) {
            throw new BusinessException("channelId: 채널을 선택해 주세요.");
        }
        findChannel(request.channelId());

        String guestName = request.guestName() == null ? "" : request.guestName().trim();
        if (guestName.isEmpty()) {
            throw new BusinessException("guestName: 게스트 이름을 입력해 주세요.");
        }
        if (guestName.length() > GUEST_NAME_MAX_LENGTH) {
            throw new BusinessException("guestName: 게스트 이름은 50자 이내로 입력해 주세요.");
        }
        String content = request.content() == null ? "" : request.content().trim();
        if (content.isEmpty()) {
            throw new BusinessException("content: 메시지 내용을 입력해 주세요.");
        }

        LocalDateTime now = LocalDateTime.now();
        MessageThread thread = threadRepository
                .findFirstByChannelIdAndGuestNameOrderByIdDesc(request.channelId(), guestName)
                .orElseGet(() -> threadRepository.save(MessageThread.create(
                        request.channelId(), guestName, matchReservation(guestName), now)));

        messageRepository.save(Message.incoming(thread.getId(), content));
        thread.receive(now);
        threadRepository.flush();

        return getDetail(thread.getId());
    }

    private Long matchReservation(String guestName) {
        List<Long> ids = threadRepository.findReservationIds(guestName, MATCHABLE_STATUSES);
        return ids.size() == 1 ? ids.get(0) : null;
    }

    private static String preview(String content) {
        return content.length() <= PREVIEW_LENGTH ? content : content.substring(0, PREVIEW_LENGTH);
    }

    private MessageThread findThread(Long threadId) {
        if (threadId == null || threadId <= 0) {
            throw new BusinessException("메시지 스레드를 찾을 수 없습니다.");
        }
        return threadRepository.findById(threadId)
                .orElseThrow(() -> new BusinessException("메시지 스레드를 찾을 수 없습니다."));
    }

    private Channel findChannel(Long channelId) {
        return channelRepository.findById(channelId)
                .orElseThrow(() -> new BusinessException("채널을 찾을 수 없습니다."));
    }
}
