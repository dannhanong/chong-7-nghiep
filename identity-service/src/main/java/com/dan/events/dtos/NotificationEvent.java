package com.dan.events.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEvent {
    private String channel;
    private String recipient;
    private String nameOfRecipient;
    private String templateCode;
    private Map<String, Object> param;
    private String subject;
    private String body;
}
