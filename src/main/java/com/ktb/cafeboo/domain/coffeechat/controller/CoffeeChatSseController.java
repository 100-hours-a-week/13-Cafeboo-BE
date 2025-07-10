package com.ktb.cafeboo.domain.coffeechat.controller;

import com.ktb.cafeboo.domain.coffeechat.service.CoffeeChatSseService;
import com.ktb.cafeboo.global.security.userdetails.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sse/coffee-chats")
public class CoffeeChatSseController {

    private final CoffeeChatSseService coffeeChatSseService;

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToCoffeeChat(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return coffeeChatSseService.subscribe(userDetails.getUserId());
    }
}
