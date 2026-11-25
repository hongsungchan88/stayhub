package com.stayhub.channel.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** 관리자 채널 동기화 로그 화면을 엽니다. */
@Controller
public class ChannelSyncLogPageController {

    /** 모의 예약 가져오기 및 로그 목록 화면을 엽니다. */
    @GetMapping("/admin/channel-sync-logs")
    public String list() {
        return "admin/channel-sync-logs/list";
    }
}