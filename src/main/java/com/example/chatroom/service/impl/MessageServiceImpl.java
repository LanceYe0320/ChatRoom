package com.example.chatroom.service.impl;

import com.example.chatroom.dto.response.MessageResponse;
import com.example.chatroom.entity.Group;
import com.example.chatroom.entity.Message;
import com.example.chatroom.entity.User;
import com.example.chatroom.entity.enums.MessageStatus;
import com.example.chatroom.entity.enums.MessageType;
import com.example.chatroom.exception.BadRequestException;
import com.example.chatroom.exception.ResourceNotFoundException;
import com.example.chatroom.repository.GroupRepository;
import com.example.chatroom.repository.MessageRepository;
import com.example.chatroom.repository.UserRepository;
import com.example.chatroom.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;

    @Override
    @Transactional
    public Message savePrivateMessage(Long senderId, Long receiverId, String content) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("发送者不存在"));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new ResourceNotFoundException("接收者不存在"));

        if (senderId.equals(receiverId)) {
            throw new BadRequestException("不能给自己发送消息");
        }

        Message message = Message.builder()
                .content(content)
                .messageType(MessageType.PRIVATE)
                .status(MessageStatus.SENT)
                .sender(sender)
                .receiver(receiver)
                .build();

        message = messageRepository.save(message);
        log.info("私聊消息已保存: {} -> {}", senderId, receiverId);
        return message;
    }

    @Override
    @Transactional
    public Message saveGroupMessage(Long senderId, Long groupId, String content) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("发送者不存在"));
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("群组不存在"));

        Message message = Message.builder()
                .content(content)
                .messageType(MessageType.GROUP)
                .status(MessageStatus.SENT)
                .sender(sender)
                .group(group)
                .build();

        message = messageRepository.save(message);
        log.info("群聊消息已保存: {} -> 群组{}", senderId, groupId);
        return message;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponse> getPrivateMessages(Long userId1, Long userId2, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Message> messages = messageRepository.findPrivateMessages(userId1, userId2, pageable);

        return messages.getContent()
                .stream()
                .map(MessageResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponse> getGroupMessages(Long groupId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Message> messages = messageRepository.findGroupMessages(groupId, pageable);

        return messages.getContent()
                .stream()
                .map(MessageResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> getOfflineMessages(Long userId, LocalDateTime lastLogoutTime) {
        return messageRepository.findOfflinePrivateMessages(userId, lastLogoutTime);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> getUnreadMessagesForUser(Long userId, LocalDateTime lastLoginTime) {
        return messageRepository.findUnreadPrivateMessagesForUser(userId, lastLoginTime);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponse> getUnreadMessages(Long userId) {
        return messageRepository.findUnreadPrivateMessages(userId)
                .stream()
                .map(MessageResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markMessagesAsRead(Long receiverId, Long senderId) {
        messageRepository.markMessagesAsRead(receiverId, senderId, LocalDateTime.now());
        log.info("已将来自用户{}的消息标记为已读", senderId);
    }

    @Override
    @Transactional
    public void markMessageAsRead(Long messageId) {
        messageRepository.updateMessageStatus(messageId, MessageStatus.READ, LocalDateTime.now());
    }

    @Override
    @Transactional
    public void updateMessageStatus(Long messageId, MessageStatus status) {
        messageRepository.updateMessageStatus(messageId, status, LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getRecentConversationUserIds(Long userId) {
        return messageRepository.findRecentConversationUserIds(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public User getCurrentUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }
}