package com.stayhub.message.controller;

import com.stayhub.account.dto.LoginUser;
import com.stayhub.common.ApiResponse;
import com.stayhub.common.PageResponse;
import com.stayhub.message.dto.InboundMessageRequest;
import com.stayhub.message.dto.MessageReplyRequest;
import com.stayhub.message.dto.MessageResponse;
import com.stayhub.message.dto.MessageThreadDetailResponse;
import com.stayhub.message.dto.MessageThreadSummaryResponse;
import com.stayhub.message.service.MessageThreadService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;

/** 통합 인박스 API (COM-001). 삭제 API 없음. */
@RestController
@RequestMapping("/api/message-threads")
public class MessageThreadController {

    private final MessageThreadService messageThreadService;

    public MessageThreadController(MessageThreadService messageThreadService) {
        this.messageThreadService = messageThreadService;
    }

    @GetMapping
    public ApiResponse<PageResponse<MessageThreadSummaryResponse>> getList(
            @RequestParam(required = false) Long channelId,
            @RequestParam(defaultValue = "0") int page
    ) {
        return ApiResponse.success(messageThreadService.getList(channelId, page));
    }

    @GetMapping("/{id}/messages")
    public ApiResponse<MessageThreadDetailResponse> getMessages(@PathVariable Long id) {
        return ApiResponse.success(messageThreadService.getDetail(id));
    }

    @PutMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable Long id) {
        messageThreadService.markRead(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/messages")
    public ApiResponse<MessageResponse> reply(
            @PathVariable Long id,
            @RequestBody MessageReplyRequest request,
            @SessionAttribute(LoginUser.SESSION_KEY) LoginUser loginUser
    ) {
        return ApiResponse.success(messageThreadService.reply(id, request, loginUser.userId()));
    }

    /**
     * 게스트 메시지 모의 수신.
     * 채널 API 연동이 없는 1차에서 수신 메시지를 넣기 위한 것으로,
     * v2 에서 채널 수집 배치로 대체될 시연용 API 입니다.
     */
    @PostMapping("/inbound")
    public ApiResponse<MessageThreadDetailResponse> inbound(@RequestBody InboundMessageRequest request) {
        return ApiResponse.success(messageThreadService.receive(request));
    }
}
