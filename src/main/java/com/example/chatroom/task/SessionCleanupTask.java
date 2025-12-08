package com.example.chatroom.task;

import com.example.chatroom.websocket.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class SessionCleanupTask {

    private final WebSocketSessionManager sessionManager;

    /**
     * 定时检查并清理失效的WebSocket会话
     * 每30秒执行一次
     */
    @Scheduled(fixedRate = 30000)
    public void cleanupInvalidSessions() {
        Map<Long, WebSocketSession> sessions = sessionManager.getAllSessions();
        int cleanedCount = 0;

        for (Map.Entry<Long, WebSocketSession> entry : sessions.entrySet()) {
            WebSocketSession session = entry.getValue();
            if (session != null && !session.isOpen()) {
                sessionManager.removeSession(entry.getKey());
                cleanedCount++;
                
                try {
                    session.close();
                } catch (IOException e) {
                    log.warn("关闭失效会话时出错: {}", e.getMessage());
                }
            }
        }

        if (cleanedCount > 0) {
            log.info("清理了 {} 个失效的WebSocket会话", cleanedCount);
        }
    }
}