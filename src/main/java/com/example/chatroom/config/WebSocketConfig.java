package com.example.chatroom.config;

import com.example.chatroom.security.JwtTokenProvider;
import com.example.chatroom.websocket.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .addInterceptors(new JwtHandshakeInterceptor())
                .setAllowedOrigins("*");
    }

    private class JwtHandshakeInterceptor implements HandshakeInterceptor {

        @Override
        public boolean beforeHandshake(ServerHttpRequest request,
                                       ServerHttpResponse response,
                                       WebSocketHandler wsHandler,
                                       Map<String, Object> attributes) {
            if (request instanceof ServletServerHttpRequest servletRequest) {
                String token = getTokenFromRequest(servletRequest.getServletRequest());

                if (token != null && jwtTokenProvider.validateToken(token)) {
                    Long userId = jwtTokenProvider.getUserIdFromToken(token);
                    String username = jwtTokenProvider.getUsernameFromToken(token);

                    attributes.put("userId", userId);
                    attributes.put("username", username);
                    attributes.put("token", token); // 保存token用于后续验证

                    log.info("WebSocket握手成功 - 用户: {}, ID: {}", username, userId);
                    return true;
                }
            }
            log.warn("WebSocket握手失败 - 无效的token");
            return false;
        }

        @Override
        public void afterHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Exception exception) {
            // 握手后处理
        }
        
        private String getTokenFromRequest(jakarta.servlet.http.HttpServletRequest request) {
            // 首先检查查询参数中的token
            String token = request.getParameter("token");
            if (token != null) {
                return token;
            }
            
            // 然后检查Authorization header
            String bearerToken = request.getHeader("Authorization");
            if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
                return bearerToken.substring(7);
            }
            
            // 最后检查Cookie中的token
            jakarta.servlet.http.Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (jakarta.servlet.http.Cookie cookie : cookies) {
                    if ("token".equals(cookie.getName())) {
                        return cookie.getValue();
                    }
                }
            }
            
            return null;
        }
    }
}