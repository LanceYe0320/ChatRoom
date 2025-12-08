package com.example.chatroom.websocket;

import com.example.chatroom.dto.response.MessageResponse;
import com.example.chatroom.entity.Message;
import com.example.chatroom.entity.User;
import com.example.chatroom.entity.enums.MessageType;
import com.example.chatroom.exception.WebSocketException;
import com.example.chatroom.repository.GroupMemberRepository;
import com.example.chatroom.repository.UserRepository;
import com.example.chatroom.service.MessageService;
import com.example.chatroom.websocket.dto.WebSocketMessage;
import com.example.chatroom.websocket.dto.WebSocketMessageType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final WebSocketSessionManager sessionManager;
    private final MessageService messageService;
    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        try {
            Long userId = getUserId(session);
            String username = getUsername(session);

            sessionManager.addSession(userId, session);

            // 更新用户在线状态
            userRepository.updateOnlineStatus(userId, true, LocalDateTime.now());

            // 加载用户的群组
            loadUserGroups(userId);

            // 通知其他在线用户
            broadcastUserStatus(userId, username, true);

            // 发送离线消息
            sendOfflineMessages(userId, session);

            log.info("用户 {}({}) 已连接", username, userId);
        } catch (Exception e) {
            log.error("处理WebSocket连接建立时发生错误", e);
            try {
                sendMessage(session, WebSocketMessage.error("连接建立失败: " + e.getMessage()));
                session.close();
            } catch (IOException ioException) {
                log.error("关闭WebSocket会话时发生错误", ioException);
            }
        }
    }

    @Override
    @Transactional
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        try {
            Long userId = getUserId(session);
            String username = getUsername(session);

            sessionManager.removeSession(userId);

            // 更新用户离线状态
            userRepository.setOffline(userId, LocalDateTime.now());

            // 通知其他在线用户
            broadcastUserStatus(userId, username, false);

            log.info("用户 {}({}) 已断开连接，状态码: {}", username, userId, status.getCode());
        } catch (Exception e) {
            log.error("处理WebSocket连接关闭时发生错误", e);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long userId = getUserId(session);
        String username = getUsername(session);

        try {
            WebSocketMessage wsMessage = objectMapper.readValue(message.getPayload(), WebSocketMessage.class);
            wsMessage.setSenderId(userId);
            wsMessage.setSenderUsername(username);
            wsMessage.setTimestamp(LocalDateTime.now());

            switch (wsMessage.getType()) {
                case PRIVATE_MESSAGE -> handlePrivateMessage(wsMessage, session);
                case GROUP_MESSAGE -> handleGroupMessage(wsMessage, session);
                case JOIN_GROUP -> handleJoinGroup(wsMessage, session);
                case LEAVE_GROUP -> handleLeaveGroup(wsMessage, session);
                case PING -> handlePing(session);
                default -> sendMessage(session, WebSocketMessage.error("未知的消息类型"));
            }
        } catch (Exception e) {
            log.error("处理消息失败: {}", e.getMessage(), e);
            sendMessage(session, WebSocketMessage.error("消息处理失败: " + e.getMessage()));
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket传输错误: {}", exception.getMessage(), exception);
        if (session.isOpen()) {
            try {
                sendMessage(session, WebSocketMessage.error("传输错误: " + exception.getMessage()));
                session.close(CloseStatus.SERVER_ERROR);
            } catch (IOException e) {
                log.error("关闭WebSocket会话时发生错误", e);
            }
        }
    }

    /**
     * 处理私聊消息
     */
    private void handlePrivateMessage(WebSocketMessage wsMessage, WebSocketSession senderSession) {
        Long senderId = wsMessage.getSenderId();
        Long receiverId = wsMessage.getReceiverId();
        String content = wsMessage.getContent();

        if (receiverId == null || content == null || content.trim().isEmpty()) {
            sendMessage(senderSession, WebSocketMessage.error("接收者ID和消息内容不能为空"));
            return;
        }

        try {
            // 保存消息到数据库
            Message savedMessage = messageService.savePrivateMessage(senderId, receiverId, content);
            MessageResponse messageResponse = MessageResponse.fromEntity(savedMessage);

            // 构建响应消息
            WebSocketMessage responseMessage = WebSocketMessage.builder()
                    .type(WebSocketMessageType.PRIVATE_MESSAGE)
                    .messageId(savedMessage.getId())
                    .senderId(senderId)
                    .senderUsername(wsMessage.getSenderUsername())
                    .senderNickname(savedMessage.getSender().getNickname())
                    .receiverId(receiverId)
                    .content(content)
                    .timestamp(savedMessage.getCreatedAt())
                    .build();

            // 发送给接收者
            WebSocketSession receiverSession = sessionManager.getSession(receiverId);
            if (receiverSession != null && receiverSession.isOpen()) {
                sendMessage(receiverSession, responseMessage);
            }

            // 发送确认给发送者
            WebSocketMessage ack = WebSocketMessage.builder()
                    .type(WebSocketMessageType.PRIVATE_MESSAGE_ACK)
                    .messageId(savedMessage.getId())
                    .receiverId(receiverId)
                    .timestamp(savedMessage.getCreatedAt())
                    .build();
            sendMessage(senderSession, ack);

            log.info("私聊消息已发送: {} -> {}", senderId, receiverId);
        } catch (Exception e) {
            log.error("处理私聊消息时发生错误", e);
            sendMessage(senderSession, WebSocketMessage.error("发送消息失败: " + e.getMessage()));
        }
    }

    /**
     * 处理群聊消息
     */
    private void handleGroupMessage(WebSocketMessage wsMessage, WebSocketSession senderSession) {
        Long senderId = wsMessage.getSenderId();
        Long groupId = wsMessage.getGroupId();
        String content = wsMessage.getContent();

        if (groupId == null || content == null || content.trim().isEmpty()) {
            sendMessage(senderSession, WebSocketMessage.error("群组ID和消息内容不能为空"));
            return;
        }

        try {
            // 检查用户是否在群组中
            if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, senderId)) {
                sendMessage(senderSession, WebSocketMessage.error("您不是该群组成员"));
                return;
            }

            // 保存消息到数据库
            Message savedMessage = messageService.saveGroupMessage(senderId, groupId, content);

            // 构建响应消息
            WebSocketMessage responseMessage = WebSocketMessage.builder()
                    .type(WebSocketMessageType.GROUP_MESSAGE)
                    .messageId(savedMessage.getId())
                    .senderId(senderId)
                    .senderUsername(wsMessage.getSenderUsername())
                    .senderNickname(savedMessage.getSender().getNickname())
                    .groupId(groupId)
                    .groupName(savedMessage.getGroup().getName())
                    .content(content)
                    .timestamp(savedMessage.getCreatedAt())
                    .build();

            // 发送给群组所有在线成员
            List<Long> memberIds = groupMemberRepository.findUserIdsByGroupId(groupId);
            for (Long memberId : memberIds) {
                if (!memberId.equals(senderId)) {
                    WebSocketSession memberSession = sessionManager.getSession(memberId);
                    if (memberSession != null && memberSession.isOpen()) {
                        sendMessage(memberSession, responseMessage);
                    }
                }
            }

            // 发送确认给发送者
            WebSocketMessage ack = WebSocketMessage.builder()
                    .type(WebSocketMessageType.GROUP_MESSAGE_ACK)
                    .messageId(savedMessage.getId())
                    .groupId(groupId)
                    .timestamp(savedMessage.getCreatedAt())
                    .build();
            sendMessage(senderSession, ack);

            log.info("群聊消息已发送: {} -> 群组{}", senderId, groupId);
        } catch (Exception e) {
            log.error("处理群聊消息时发生错误", e);
            sendMessage(senderSession, WebSocketMessage.error("发送消息失败: " + e.getMessage()));
        }
    }

    /**
     * 处理加入群组
     */
    private void handleJoinGroup(WebSocketMessage wsMessage, WebSocketSession session) {
        try {
            Long userId = wsMessage.getSenderId();
            Long groupId = wsMessage.getGroupId();

            sessionManager.addUserToGroup(groupId, userId);

            // 通知群组其他成员
            WebSocketMessage notification = WebSocketMessage.builder()
                    .type(WebSocketMessageType.GROUP_NOTIFICATION)
                    .senderId(userId)
                    .senderUsername(wsMessage.getSenderUsername())
                    .groupId(groupId)
                    .content(wsMessage.getSenderUsername() + " 加入了群组")
                    .timestamp(LocalDateTime.now())
                    .build();

            Set<Long> onlineMembers = sessionManager.getOnlineGroupMembers(groupId);
            for (Long memberId : onlineMembers) {
                if (!memberId.equals(userId)) {
                    WebSocketSession memberSession = sessionManager.getSession(memberId);
                    if (memberSession != null && memberSession.isOpen()) {
                        sendMessage(memberSession, notification);
                    }
                }
            }
        } catch (Exception e) {
            log.error("处理加入群组时发生错误", e);
            sendMessage(session, WebSocketMessage.error("加入群组失败: " + e.getMessage()));
        }
    }

    /**
     * 处理离开群组
     */
    private void handleLeaveGroup(WebSocketMessage wsMessage, WebSocketSession session) {
        try {
            Long userId = wsMessage.getSenderId();
            Long groupId = wsMessage.getGroupId();

            // 通知群组其他成员
            WebSocketMessage notification = WebSocketMessage.builder()
                    .type(WebSocketMessageType.GROUP_NOTIFICATION)
                    .senderId(userId)
                    .senderUsername(wsMessage.getSenderUsername())
                    .groupId(groupId)
                    .content(wsMessage.getSenderUsername() + " 离开了群组")
                    .timestamp(LocalDateTime.now())
                    .build();

            Set<Long> onlineMembers = sessionManager.getOnlineGroupMembers(groupId);
            for (Long memberId : onlineMembers) {
                if (!memberId.equals(userId)) {
                    WebSocketSession memberSession = sessionManager.getSession(memberId);
                    if (memberSession != null && memberSession.isOpen()) {
                        sendMessage(memberSession, notification);
                    }
                }
            }

            sessionManager.removeUserFromGroup(groupId, userId);
        } catch (Exception e) {
            log.error("处理离开群组时发生错误", e);
            sendMessage(session, WebSocketMessage.error("离开群组失败: " + e.getMessage()));
        }
    }

    /**
     * 处理心跳
     */
    private void handlePing(WebSocketSession session) {
        sendMessage(session, WebSocketMessage.pong());
    }

    /**
     * 加载用户的群组
     */
    private void loadUserGroups(Long userId) {
        try {
            List<Long> groupIds = groupMemberRepository.findByUserIdWithGroup(userId)
                    .stream()
                    .map(gm -> gm.getGroup().getId())
                    .toList();

            for (Long groupId : groupIds) {
                sessionManager.addUserToGroup(groupId, userId);
            }
        } catch (Exception e) {
            log.error("加载用户群组时发生错误", e);
        }
    }

    /**
     * 发送离线消息
     */
    private void sendOfflineMessages(Long userId, WebSocketSession session) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null || user.getLastLogoutTime() == null) {
                return;
            }

            List<Message> offlineMessages = messageService.getOfflineMessages(userId, user.getLastLogoutTime());

            for (Message message : offlineMessages) {
                WebSocketMessage wsMessage = WebSocketMessage.builder()
                        .type(message.getMessageType() == MessageType.PRIVATE
                                ? WebSocketMessageType.PRIVATE_MESSAGE
                                : WebSocketMessageType.GROUP_MESSAGE)
                        .messageId(message.getId())
                        .senderId(message.getSender().getId())
                        .senderUsername(message.getSender().getUsername())
                        .senderNickname(message.getSender().getNickname())
                        .content(message.getContent())
                        .timestamp(message.getCreatedAt())
                        .build();

                if (message.getReceiver() != null) {
                    wsMessage.setReceiverId(message.getReceiver().getId());
                }
                if (message.getGroup() != null) {
                    wsMessage.setGroupId(message.getGroup().getId());
                    wsMessage.setGroupName(message.getGroup().getName());
                }

                sendMessage(session, wsMessage);
            }

            if (!offlineMessages.isEmpty()) {
                log.info("已发送 {} 条离线消息给用户 {}", offlineMessages.size(), userId);
            }
        } catch (Exception e) {
            log.error("发送离线消息时发生错误", e);
        }
    }

    /**
     * 广播用户状态变化
     */
    private void broadcastUserStatus(Long userId, String username, boolean online) {
        try {
            WebSocketMessage statusMessage = online
                    ? WebSocketMessage.userOnline(userId, username)
                    : WebSocketMessage.userOffline(userId, username);

            Map<Long, WebSocketSession> allSessions = sessionManager.getAllSessions();
            for (Map.Entry<Long, WebSocketSession> entry : allSessions.entrySet()) {
                if (!entry.getKey().equals(userId) && entry.getValue().isOpen()) {
                    sendMessage(entry.getValue(), statusMessage);
                }
            }
        } catch (Exception e) {
            log.error("广播用户状态时发生错误", e);
        }
    }

    /**
     * 发送消息
     */
    private void sendMessage(WebSocketSession session, WebSocketMessage message) {
        try {
            if (session.isOpen()) {
                String json = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(json));
            }
        } catch (IOException e) {
            log.error("发送消息失败: {}", e.getMessage());
            // 如果发送失败，尝试关闭会话
            try {
                if (session.isOpen()) {
                    session.close();
                }
            } catch (IOException ioException) {
                log.error("关闭WebSocket会话时发生错误", ioException);
            }
        } catch (Exception e) {
            log.error("发送消息时发生未知错误", e);
        }
    }

    private Long getUserId(WebSocketSession session) {
        return (Long) session.getAttributes().get("userId");
    }

    private String getUsername(WebSocketSession session) {
        return (String) session.getAttributes().get("username");
    }
}