package com.stayhub.channel;

import static org.assertj.core.api.Assertions.assertThat;

import com.stayhub.channel.dto.ChannelResponse;
import com.stayhub.channel.service.ChannelService;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ChannelServiceTest {

    @Autowired
    private ChannelService channelService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("저장된 채널의 모든 값이 목록에 나온다")
    void savedChannelAppearsInList() {
        String channelName = "test-" + UUID.randomUUID();

        // 등록 API가 아직 없으므로 테스트 데이터만 SQL로 넣습니다.
        jdbcTemplate.update("""
                INSERT INTO channels (
                    channel_name,
                    commission_rate,
                    is_registered,
                    default_commission_rate,
                    created_at,
                    updated_at
                ) VALUES (?, ?, ?, ?, NOW(), NOW())
                """, channelName, "10.25", false, "12.50");

        Long id = jdbcTemplate.queryForObject(
                "SELECT channel_id FROM channels WHERE channel_name = ?",
                Long.class,
                channelName
        );

        ChannelResponse found = channelService.getList()
                .stream()
                .filter(channel -> channel.id().equals(id))
                .findFirst()
                .orElseThrow();

        assertThat(found.id()).isEqualTo(id);
        assertThat(found.channelName()).isEqualTo(channelName);
        assertThat(found.commissionRate()).isEqualByComparingTo("10.25");
        assertThat(found.isRegistered()).isFalse();
        assertThat(found.defaultCommissionRate())
                .isEqualByComparingTo("12.50");
        assertThat(found.createdAt()).isNotNull();
        assertThat(found.updatedAt()).isNotNull();
    }
}
