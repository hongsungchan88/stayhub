package com.stayhub.channel.service;

import com.stayhub.channel.dto.ChannelResponse;
import com.stayhub.channel.repository.ChannelRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ChannelService {

    private final ChannelRepository channelRepository;

    public ChannelService(ChannelRepository channelRepository) {
        this.channelRepository = channelRepository;
    }

    public List<ChannelResponse> getList() {
        return channelRepository.findAll()
                .stream()
                .map(ChannelResponse::from)
                .toList();
    }
}
