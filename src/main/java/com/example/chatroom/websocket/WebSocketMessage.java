package com.example.chatroom.websocket;

import com.example.chatroom.websocket.dto.WebSocketMessageType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebSocketMessage {

    private WebSocketMessageType type;
    private Long senderId;
    private String senderUsername;
    private String senderNickname;
    private Long receiverId;
    private Long groupId;
    private String groupName;
    private String content;
    private Long messageId;
    private LocalDateTime timestamp;

    public static WebSocketMessage error(String message) {
        return WebSocketMessage.builder()
                .type(WebSocketMessageType.ERROR)
                .content(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static WebSocketMessage system(String message) {
        return WebSocketMessage.builder()
                .type(WebSocketMessageType.SYSTEM)
                .content(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static WebSocketMessage userOnline(Long userId, String username) {
        return WebSocketMessage.builder()
                .type(WebSocketMessageType.USER_ONLINE)
                .senderId(userId)
                .senderUsername(username)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static WebSocketMessage userOffline(Long userId, String username) {
        return WebSocketMessage.builder()
                .type(WebSocketMessageType.USER_OFFLINE)
                .senderId(userId)
                .senderUsername(username)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static WebSocketMessage pong() {
        return WebSocketMessage.builder()
                .type(WebSocketMessageType.PONG)
                .timestamp(LocalDateTime.now())
                .build();
    }
}