package com.example.chatroom.service;

import com.example.chatroom.dto.response.MessageResponse;
import com.example.chatroom.entity.Message;
import com.example.chatroom.entity.User;

import java.time.LocalDateTime;
import java.util.List;

public interface MessageService {

    Message savePrivateMessage(Long senderId, Long receiverId, String content);

    Message saveGroupMessage(Long senderId, Long groupId, String content);

    List<MessageResponse> getPrivateMessages(Long userId1, Long userId2, int page, int size);

    List<MessageResponse> getGroupMessages(Long groupId, int page, int size);

    List<Message> getOfflineMessages(Long userId, LocalDateTime lastLogoutTime);

    List<Message> getUnreadMessagesForUser(Long userId, LocalDateTime lastLoginTime);

    List<MessageResponse> getUnreadMessages(Long userId);

    void markMessagesAsRead(Long receiverId, Long senderId);

    void markMessageAsRead(Long messageId);

    void updateMessageStatus(Long messageId, com.example.chatroom.entity.enums.MessageStatus status);

    List<Long> getRecentConversationUserIds(Long userId);

    User getCurrentUserById(Long userId);
}
