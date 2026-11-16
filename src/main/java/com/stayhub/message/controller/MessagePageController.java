package com.stayhub.message.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** 통합 인박스 화면 SCR-COM-01. */
@Controller
@RequestMapping("/admin/messages")
public class MessagePageController {

    @GetMapping
    public String inbox() {
        return "admin/messages/inbox";
    }
}
