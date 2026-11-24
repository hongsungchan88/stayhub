package com.stayhub.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stayhub.account.dto.LoginUser;
import com.stayhub.account.entity.UserRole;
import com.stayhub.channel.dto.MockChannelFeed;
import com.stayhub.channel.repository.ChannelRepository;
import com.stayhub.channel.repository.ChannelReservationLinkRepository;
import com.stayhub.channel.service.MockChannelClient;
import com.stayhub.common.PropertyScope;
import com.stayhub.room.entity.Room;
import com.stayhub.room.repository.RoomRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional(isolation = Isolation.READ_COMMITTED)
class MockReservationImportTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired ChannelRepository channels;
    @Autowired ChannelReservationLinkRepository links;
    @Autowired RoomRepository rooms;
    @MockitoBean MockChannelClient client;

    String externalId, roomNumber;
    Long channelId, roomId;
    final LoginUser host = new LoginUser(1L, "tester", "테스트", UserRole.HOST);

    @BeforeEach void prepare() {
        if (channels.findAllByChannelNameAndRegisteredTrue("MOCK_A").isEmpty()) {
            jdbc.update("""
                    INSERT INTO channels (channel_name, commission_rate, is_registered,
                        default_commission_rate, created_at, updated_at)
                    VALUES ('MOCK_A', 0, true, 10, NOW(), NOW())
                    """);
        }
        channelId = channels.findAllByChannelNameAndRegisteredTrue("MOCK_A").getFirst().getId();
        String tag = UUID.randomUUID().toString().substring(0, 8);
        externalId = "A-" + tag;
        roomNumber = "CHN-" + tag;
        roomId = rooms.saveAndFlush(Room.create(PropertyScope.DEFAULT_PROPERTY_ID,
                roomNumber, "STANDARD", new BigDecimal("100000"), 2)).getId();
        feed(1, "CONFIRMED");
    }

    private void feed(int revision, String status) {
        LocalDate date = LocalDate.now().plusDays(10);
        var b = new MockChannelFeed.Booking("MOCK_A", externalId, revision, status,
                roomNumber, "Mock Guest", "010-0000-0000", date, date.plusDays(2), 1,
                OffsetDateTime.now());
        when(client.readChannelA()).thenReturn(List.of(b));
    }

    private String url() { return "/api/channel-sync/mock-a/import/" + externalId; }

    private long countBookings() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM reservations WHERE room_id = ? AND channel_id = ?",
                Long.class, roomId, channelId);
    }

    @Test void replayReturnsSameReservation() throws Exception {
        mvc.perform(get("/api/channel-sync/mock-a/preview").sessionAttr(LoginUser.SESSION_KEY, host))
                .andExpect(status().isOk());
        assertThat(countBookings()).isZero();

        var first = mvc.perform(post(url()).sessionAttr(LoginUser.SESSION_KEY, host))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.outcome").value("CREATED"))
                .andExpect(jsonPath("$.data.createdCount").value(1)).andReturn();
        long id = json.readTree(first.getResponse().getContentAsByteArray()).at("/data/reservationId").asLong();
        assertThat(id).isPositive();

        var second = mvc.perform(post(url()).sessionAttr(LoginUser.SESSION_KEY, host))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.outcome").value("ALREADY_IMPORTED"))
                .andExpect(jsonPath("$.data.createdCount").value(0)).andReturn();
        assertThat(json.readTree(second.getResponse().getContentAsByteArray()).at("/data/reservationId").asLong())
                .isEqualTo(id);
        assertThat(countBookings()).isOne();
        assertThat(links.findByChannelIdAndExternalReservationId(channelId, externalId).orElseThrow()
                .getReservationId()).isEqualTo(id);
    }

    @Test void higherRevisionIsNotMarkedApplied() throws Exception {
        mvc.perform(post(url()).sessionAttr(LoginUser.SESSION_KEY, host)).andExpect(status().isOk());
        feed(2, "CANCELLED");
        mvc.perform(post(url()).sessionAttr(LoginUser.SESSION_KEY, host)).andExpect(status().isBadRequest());
        assertThat(countBookings()).isOne();
        assertThat(links.findByChannelIdAndExternalReservationId(channelId, externalId).orElseThrow()
                .getAppliedRevision()).isOne();
    }

    @Test void importRequiresHost() throws Exception {
        mvc.perform(post(url())).andExpect(status().isUnauthorized());
        var front = new LoginUser(2L, "front", "프런트", UserRole.FRONT);
        mvc.perform(post(url()).sessionAttr(LoginUser.SESSION_KEY, front)).andExpect(status().isForbidden());
        assertThat(countBookings()).isZero();
    }
}