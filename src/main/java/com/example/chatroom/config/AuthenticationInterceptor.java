package com.example.chatroom.config;

import com.example.chatroom.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuthenticationInterceptor implements HandlerInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean preHandle(HttpServletRequest request, 
                             HttpServletResponse response, 
                             Object handler) throws Exception {
        
        // 允许访问认证相关接口和公共资源
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/auth/") || 
            uri.startsWith("/ws/") || 
            uri.equals("/favicon.ico") ||
            uri.startsWith("/h2-console/")) {
            return true;
        }
        
        // 对于根路径和静态资源，需要特殊处理
        if (uri.equals("/") || uri.equals("/index.html") || 
            uri.startsWith("/css/") || uri.startsWith("/js/")) {
            // 检查是否有有效的JWT token
            String token = getTokenFromRequest(request);
            if (token != null && jwtTokenProvider.validateToken(token)) {
                // 如果token有效，则允许访问
                return true;
            }
            // 如果token无效或者没有token，则继续允许访问（这样用户可以看到登录页面）
            // 但是在前端需要检查认证状态
            return true;
        }
        
        // 检查是否有有效的JWT token
        String token = getTokenFromRequest(request);
        if (token != null && jwtTokenProvider.validateToken(token)) {
            return true;
        }
        
        // 重定向到登录页面
        response.sendRedirect("/");
        return false;
    }
    
    private String getTokenFromRequest(HttpServletRequest request) {
        // 首先检查Authorization header
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        // 然后检查Cookie中的token
        String cookieHeader = request.getHeader("Cookie");
        if (cookieHeader != null) {
            String[] cookies = cookieHeader.split("; ");
            for (String cookie : cookies) {
                if (cookie.startsWith("token=")) {
                    return cookie.substring(6);
                }
            }
        }
        
        // 最后检查查询参数中的token
        return request.getParameter("token");
    }
}