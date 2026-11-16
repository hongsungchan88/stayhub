package com.stayhub.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stayhub.account.dto.LoginUser;
import com.stayhub.account.entity.UserRole;
import com.stayhub.channel.entity.Channel;
import com.stayhub.channel.repository.ChannelRepository;
import com.stayhub.common.PropertyScope;
import com.stayhub.message.dto.InboundMessageRequest;
import com.stayhub.message.dto.MessageReplyRequest;
import com.stayhub.message.dto.MessageThreadDetailResponse;
import com.stayhub.message.dto.MessageThreadSummaryResponse;
import com.stayhub.message.entity.Message;
import com.stayhub.message.entity.MessageDirection;
import com.stayhub.message.entity.SendStatus;
import com.stayhub.message.repository.MessageRepository;
import com.stayhub.message.repository.MessageThreadRepository;
import com.stayhub.message.service.MessageSender;
import com.stayhub.message.service.MessageThreadService;
import com.stayhub.reservation.service.ReservationService;
import com.stayhub.room.entity.Room;
import com.stayhub.room.repository.RoomRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 통합 인박스 API (COM-001).
 * data.sql 샘플 스레드가 있으므로, 테스트마다 새 채널과 태그 붙은 게스트 이름으로 자기 데이터만 확인합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MessageThreadApiTest {

    private static final long STAFF_ID = 7L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MessageThreadService service;

    @Autowired
    private MessageThreadRepository threadRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private RoomRepository roomRepository;

    @MockitoSpyBean
    private MessageSender messageSender;

    private String tag;
    private Long channelId;

    @BeforeEach
    void setUp() {
        tag = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        channelId = saveChannel("인박스채널" + tag);
    }

    @Test
    @DisplayName("목록은 마지막 메시지 시각 최신순이고 채널로 거른다")
    void listSortedAndFiltered() {
        Long first = inbound(channelId, "A" + tag, "첫 번째").threadId();
        Long second = inbound(channelId, "B" + tag, "두 번째").threadId();
        Long otherChannelThread = inbound(saveChannel("다른채널" + tag), "C" + tag, "다른 채널").threadId();

        List<Long> ids = ids(service.getList(channelId, 0).content());
        assertThat(ids).containsExactly(second, first).doesNotContain(otherChannelThread);

        // 답장하면 그 스레드가 맨 위로 올라갑니다.
        service.reply(first, new MessageReplyRequest("답장"), STAFF_ID);
        assertThat(ids(service.getList(channelId, 0).content())).containsExactly(first, second);

        // 전체 목록에도 채널명이 붙어 나옵니다.
        assertThat(service.getList(null, 0).content())
                .filteredOn(item -> item.threadId().equals(otherChannelThread))
                .singleElement()
                .satisfies(item -> assertThat(item.channelName()).isEqualTo("다른채널" + tag));
    }

    @Test
    @DisplayName("미리보기는 50자, 마지막이 게스트 메시지면 needsReply")
    void previewAndNeedsReply() {
        String longText = "가".repeat(60);
        Long threadId = inbound(channelId, "A" + tag, longText).threadId();

        MessageThreadSummaryResponse item = service.getList(channelId, 0).content().get(0);
        assertThat(item.lastMessagePreview()).isEqualTo("가".repeat(50));
        assertThat(item.needsReply()).isTrue();
        assertThat(item.isRead()).isFalse();
        assertThat(item.channelName()).isEqualTo("인박스채널" + tag);

        service.reply(threadId, new MessageReplyRequest("확인했습니다."), STAFF_ID);
        item = service.getList(channelId, 0).content().get(0);
        assertThat(item.needsReply()).isFalse();
        assertThat(item.lastMessagePreview()).isEqualTo("확인했습니다.");
    }

    @Test
    @DisplayName("같은 채널·게스트는 기존 스레드에 붙고, 새 게스트는 새 스레드")
    void inboundThreading() {
        Long threadId = inbound(channelId, "A" + tag, "첫 메시지").threadId();
        service.markRead(threadId);

        MessageThreadDetailResponse again = inbound(channelId, "A" + tag, "두 번째 메시지");
        assertThat(again.threadId()).isEqualTo(threadId);
        assertThat(again.isRead()).isFalse(); // 새 메시지가 오면 다시 미읽음
        assertThat(again.messages()).extracting("content").containsExactly("첫 메시지", "두 번째 메시지");
        assertThat(again.messages()).allSatisfy(m -> assertThat(m.direction()).isEqualTo(MessageDirection.IN));

        assertThat(inbound(channelId, "B" + tag, "다른 게스트").threadId()).isNotEqualTo(threadId);
        // 같은 게스트라도 채널이 다르면 다른 스레드
        assertThat(inbound(saveChannel("다른채널" + tag), "A" + tag, "다른 채널").threadId()).isNotEqualTo(threadId);
    }

    @Test
    @DisplayName("진행 중 예약이 정확히 1건일 때만 스레드에 예약을 연결한다")
    void reservationMatching() {
        Long roomId = roomRepository.save(Room.create(
                PropertyScope.DEFAULT_PROPERTY_ID, "M" + tag, "STANDARD", new BigDecimal("100000.00"), 2)).getId();
        LocalDate day = LocalDate.of(2071, 1, 1);

        Long reservationId = reservationService.create(
                null, "One" + tag, "010-1111-1111", roomId, null, day, day.plusDays(1), 1).getId();
        reservationService.create(null, "Two" + tag, "010-1111-1111", roomId, null, day.plusDays(1), day.plusDays(2), 1);
        reservationService.create(null, "Two" + tag, "010-1111-1111", roomId, null, day.plusDays(2), day.plusDays(3), 1);

        MessageThreadDetailResponse one = inbound(channelId, "One" + tag, "예약 1건");
        assertThat(one.reservationId()).isEqualTo(reservationId);
        assertThat(one.reservationCode()).isNotBlank();

        assertThat(inbound(channelId, "Two" + tag, "예약 2건").reservationId()).isNull();
        assertThat(inbound(channelId, "None" + tag, "예약 없음").reservationId()).isNull();
    }

    @Test
    @DisplayName("없는 채널로 모의 수신하면 400")
    void inboundUnknownChannel() throws Exception {
        mockMvc.perform(post("/api/message-threads/inbound")
                        .session(session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new InboundMessageRequest(Long.MAX_VALUE, "A" + tag, "안녕하세요"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("채널을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("답장은 OUT·SUCCESS·보낸 직원으로 저장되고 발송기를 부르며 last_message_at 을 갱신한다")
    void reply() throws Exception {
        Long threadId = inbound(channelId, "A" + tag, "문의드립니다").threadId();
        var before = threadRepository.findById(threadId).orElseThrow().getLastMessageAt();

        mockMvc.perform(post("/api/message-threads/{id}/messages", threadId)
                        .session(session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "  답변드립니다.  "))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.direction").value("OUT"))
                .andExpect(jsonPath("$.data.sendStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.data.sentBy").value(STAFF_ID))
                .andExpect(jsonPath("$.data.content").value("답변드립니다."));

        verify(messageSender).send("인박스채널" + tag, "A" + tag, "답변드립니다.");

        List<Message> messages = messageRepository.findByThreadIdOrderByCreatedAtAscIdAsc(threadId);
        Message out = messages.get(messages.size() - 1);
        assertThat(out.getDirection()).isEqualTo(MessageDirection.OUT);
        assertThat(out.getSendStatus()).isEqualTo(SendStatus.SUCCESS);
        assertThat(out.getSentBy()).isEqualTo(STAFF_ID);
        assertThat(out.getRetryCount()).isZero();
        assertThat(threadRepository.findById(threadId).orElseThrow().getLastMessageAt()).isAfterOrEqualTo(before);
    }

    @Test
    @DisplayName("공백·1,001자 답장은 400, 1,000자는 저장")
    void replyValidation() throws Exception {
        Long threadId = inbound(channelId, "A" + tag, "문의").threadId();

        for (String content : new String[] {"   ", "가".repeat(1001)}) {
            mockMvc.perform(post("/api/message-threads/{id}/messages", threadId)
                            .session(session())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("content", content))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        mockMvc.perform(post("/api/message-threads/{id}/messages", threadId)
                        .session(session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "가".repeat(1000)))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("읽음 처리 후 대화 조회, 없는 스레드는 400")
    void readAndMissingThread() throws Exception {
        Long threadId = inbound(channelId, "A" + tag, "읽어 주세요").threadId();

        mockMvc.perform(put("/api/message-threads/{id}/read", threadId).session(session()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/message-threads/{id}/messages", threadId).session(session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isRead").value(true))
                .andExpect(jsonPath("$.data.channelName").value("인박스채널" + tag))
                .andExpect(jsonPath("$.data.messages[0].content").value("읽어 주세요"));

        mockMvc.perform(get("/api/message-threads/99999/messages").session(session()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("메시지 스레드를 찾을 수 없습니다."));
        mockMvc.perform(put("/api/message-threads/99999/read").session(session()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("로그인 없이 /api/message-threads 는 401")
    void withoutLoginIs401() throws Exception {
        mockMvc.perform(get("/api/message-threads"))
                .andExpect(status().isUnauthorized());
    }

    private MessageThreadDetailResponse inbound(Long channel, String guestName, String content) {
        return service.receive(new InboundMessageRequest(channel, guestName, content));
    }

    private static List<Long> ids(List<MessageThreadSummaryResponse> items) {
        return items.stream().map(MessageThreadSummaryResponse::threadId).toList();
    }

    /** Channel 은 채널 도메인 엔티티라 만드는 메서드가 없어 테스트에서만 값을 넣습니다(payment 의 ChannelFixture 와 같은 방식). */
    private Long saveChannel(String name) {
        Channel channel = BeanUtils.instantiateClass(Channel.class);
        ReflectionTestUtils.setField(channel, "channelName", name);
        return channelRepository.save(channel).getId();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private MockHttpSession session() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(LoginUser.SESSION_KEY, new LoginUser(STAFF_ID, "front", "프런트", UserRole.FRONT));
        return session;
    }
}
