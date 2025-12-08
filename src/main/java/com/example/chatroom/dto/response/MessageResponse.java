package com.example.chatroom.dto.response;

import com.example.chatroom.entity.Message;
import com.example.chatroom.entity.enums.MessageStatus;
import com.example.chatroom.entity.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {

    private Long id;
    private String content;
    private MessageType messageType;
    private MessageStatus status;
    private Long senderId;
    private String senderUsername;
    private String senderNickname;
    private Long receiverId;
    private String receiverUsername;
    private Long groupId;
    private String groupName;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;

    public static MessageResponse fromEntity(Message message) {
        MessageResponseBuilder builder = MessageResponse.builder()
                .id(message.getId())
                .content(message.getContent())
                .messageType(message.getMessageType())
                .status(message.getStatus())
                .senderId(message.getSender().getId())
                .senderUsername(message.getSender().getUsername())
                .senderNickname(message.getSender().getNickname())
                .createdAt(message.getCreatedAt())
                .readAt(message.getReadAt());

        if (message.getReceiver() != null) {
            builder.receiverId(message.getReceiver().getId())
                    .receiverUsername(message.getReceiver().getUsername());
        }

        if (message.getGroup() != null) {
            builder.groupId(message.getGroup().getId())
                    .groupName(message.getGroup().getName());
        }

        return builder.build();
    }
}
