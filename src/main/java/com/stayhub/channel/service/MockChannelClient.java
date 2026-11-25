package com.stayhub.channel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stayhub.channel.dto.MockChannelFeed;
import com.stayhub.common.BusinessException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/** 앱에 포함된 JSON 파일에서 모의 채널 A의 예약 목록을 읽습니다. */
@Service
@RequiredArgsConstructor
public class MockChannelClient {

    private static final String MOCK_A_PATH = "mock-channel/mock-a.json";
    private final ObjectMapper objectMapper;

    /** 모의 채널 A의 예약 목록을 반환합니다. */
    public List<MockChannelFeed.Booking> readChannelA() {
        ClassPathResource resource = new ClassPathResource(MOCK_A_PATH);

        try (InputStream input = resource.getInputStream()) {
            MockChannelFeed feed = objectMapper.readValue(input, MockChannelFeed.class);

            if (feed == null || !feed.success() || feed.data() == null) {
                throw new BusinessException("모의 채널 A의 예약 목록 형식이 올바르지 않습니다.");
            }

            return feed.data();
        } catch (IOException exception) {
            throw new BusinessException("모의 채널 A의 예약 목록을 불러오지 못했습니다.", exception);
        }
    }
}