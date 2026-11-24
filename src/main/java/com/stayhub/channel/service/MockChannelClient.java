package com.stayhub.channel.service;

import com.stayhub.channel.dto.MockChannelFeed;
import com.stayhub.common.BusinessException;
import java.util.List;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class MockChannelClient {

    private static final String MOCK_A_URL =
            "http://127.0.0.1:18081/mock-a/reservations.json";

    private final RestClient client;

    public MockChannelClient(RestClient.Builder builder) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        this.client = builder.requestFactory(factory).build();
    }

    public List<MockChannelFeed.Booking> readChannelA() {
        MockChannelFeed feed;

        try {
            feed = client.get()
                    .uri(MOCK_A_URL)
                    .retrieve()
                    .body(MockChannelFeed.class);
        } catch (RestClientException e) {
            throw new BusinessException(
                    "채널 A의 예약을 읽지 못했습니다. 가짜 채널 서버와 응답 내용을 확인해 주세요.", e);
        }

        if (feed == null || !feed.success() || feed.data() == null) {
            throw new BusinessException("채널 A의 예약 목록 응답이 올바르지 않습니다.");
        }

        return feed.data();
    }
}