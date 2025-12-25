package com.example.chatroom.controller;

import com.example.chatroom.dto.request.GroupMessageRequest;
import com.example.chatroom.dto.request.PrivateMessageRequest;
import com.example.chatroom.dto.response.ApiResponse;
import com.example.chatroom.dto.response.MessageResponse;
import com.example.chatroom.security.CustomUserDetails;
import com.example.chatroom.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MessageController {

    private final MessageService messageService;

    @GetMapping("/private/{userId}")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getPrivateMessages(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<MessageResponse> messages = messageService.getPrivateMessages(userDetails.getUserId(), userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getGroupMessages(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<MessageResponse> messages = messageService.getGroupMessages(groupId, page, size);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getUnreadMessages(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<MessageResponse> messages = messageService.getUnreadMessages(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    @PutMapping("/{messageId}/read")
    public ResponseEntity<ApiResponse<Void>> markMessageAsRead(@PathVariable Long messageId) {
        messageService.markMessageAsRead(messageId);
        return ResponseEntity.ok(ApiResponse.success("消息已标记为已读"));
    }

    @PutMapping("/read/{senderId}")
    public ResponseEntity<ApiResponse<Void>> markMessagesAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long senderId) {
        messageService.markMessagesAsRead(userDetails.getUserId(), senderId);
        return ResponseEntity.ok(ApiResponse.success("消息已标记为已读"));
    }
    
    @GetMapping("/offline")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getOfflineMessages(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        // 获取用户上次登录时间，如果没有则使用账户创建时间
        var currentUser = userDetails.getUserId();
        var user = messageService.getCurrentUserById(currentUser);
        var lastLoginTime = user.getLastLoginTime();
        
        if (lastLoginTime == null) {
            lastLoginTime = user.getCreatedAt();
        }
        
        var messages = messageService.getUnreadMessagesForUser(
            userDetails.getUserId(), 
            lastLoginTime
        );
        
        List<MessageResponse> response = messages.stream()
                .map(MessageResponse::fromEntity)
                .toList();
                
        return ResponseEntity.ok(ApiResponse.success("获取离线消息成功", response));
    }
    
    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<Long>>> getConversationUserIds(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<Long> userIds = messageService.getRecentConversationUserIds(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("获取对话用户列表成功", userIds));
    }
}