package com.stayhub.channel.controller;

import com.stayhub.channel.dto.ChannelResponse;
import com.stayhub.channel.service.ChannelService;
import com.stayhub.common.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/channels")
public class ChannelController {

    private final ChannelService channelService;

    public ChannelController(ChannelService channelService) {
        this.channelService = channelService;
    }

    @GetMapping
    public ApiResponse<List<ChannelResponse>> getList() {
        return ApiResponse.success(channelService.getList());
    }
}
