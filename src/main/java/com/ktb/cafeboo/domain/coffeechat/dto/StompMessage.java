package com.ktb.cafeboo.domain.coffeechat.dto;

import com.ktb.cafeboo.global.enums.MessageType;
import lombok.Data;

@Data
public class StompMessage {
    private String senderId;
    private String coffeechatId;
    private String message;
    private MessageType type;
}
