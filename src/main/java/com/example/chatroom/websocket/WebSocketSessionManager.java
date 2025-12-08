package com.example.chatroom.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@Slf4j
public class WebSocketSessionManager {

    // 用户ID到会话的映射
    private final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    
    // 群组成员管理：群组ID -> 成员用户ID集合
    private final Map<Long, Set<Long>> groupMembers = new ConcurrentHashMap<>();

    /**
     * 添加用户会话
     */
    public void addSession(Long userId, WebSocketSession session) {
        userSessions.put(userId, session);
        log.debug("用户会话已添加: userId={}", userId);
    }

    /**
     * 移除用户会话
     */
    public void removeSession(Long userId) {
        WebSocketSession removed = userSessions.remove(userId);
        if (removed != null) {
            // 从所有群组中移除该用户
            for (Set<Long> members : groupMembers.values()) {
                members.remove(userId);
            }
            log.debug("用户会话已移除: userId={}", userId);
        }
    }

    /**
     * 获取用户会话
     */
    public WebSocketSession getSession(Long userId) {
        return userSessions.get(userId);
    }

    /**
     * 获取所有用户会话
     */
    public Map<Long, WebSocketSession> getAllSessions() {
        return new ConcurrentHashMap<>(userSessions);
    }

    /**
     * 添加用户到群组
     */
    public void addUserToGroup(Long groupId, Long userId) {
        groupMembers.computeIfAbsent(groupId, k -> new CopyOnWriteArraySet<>()).add(userId);
        log.debug("用户已添加到群组: userId={}, groupId={}", userId, groupId);
    }

    /**
     * 从群组移除用户
     */
    public void removeUserFromGroup(Long groupId, Long userId) {
        Set<Long> members = groupMembers.get(groupId);
        if (members != null) {
            members.remove(userId);
            log.debug("用户已从群组移除: userId={}, groupId={}", userId, groupId);
        }
    }

    /**
     * 获取群组在线成员
     */
    public Set<Long> getOnlineGroupMembers(Long groupId) {
        Set<Long> members = groupMembers.get(groupId);
        if (members != null) {
            // 只返回在线的成员
            return members.stream()
                    .filter(userSessions::containsKey)
                    .collect(java.util.stream.Collectors.toSet());
        }
        return Set.of();
    }

    /**
     * 获取用户所在的所有群组
     */
    public Set<Long> getUserGroups(Long userId) {
        Set<Long> userGroups = new java.util.HashSet<>();
        for (Map.Entry<Long, Set<Long>> entry : groupMembers.entrySet()) {
            if (entry.getValue().contains(userId)) {
                userGroups.add(entry.getKey());
            }
        }
        return userGroups;
    }
}