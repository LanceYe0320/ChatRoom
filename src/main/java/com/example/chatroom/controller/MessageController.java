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
}