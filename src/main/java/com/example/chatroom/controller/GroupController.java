package com.example.chatroom.controller;

import com.example.chatroom.dto.request.CreateGroupRequest;
import com.example.chatroom.dto.request.JoinGroupRequest;
import com.example.chatroom.dto.response.ApiResponse;
import com.example.chatroom.dto.response.GroupResponse;
import com.example.chatroom.dto.response.UserResponse;
import com.example.chatroom.security.CustomUserDetails;
import com.example.chatroom.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GroupController {

    private final GroupService groupService;

    @PostMapping
    public ResponseEntity<ApiResponse<GroupResponse>> createGroup(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateGroupRequest request) {
        GroupResponse group = groupService.createGroup(userDetails.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("群组创建成功", group));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GroupResponse>> getGroup(@PathVariable Long id) {
        GroupResponse group = groupService.getGroupById(id);
        return ResponseEntity.ok(ApiResponse.success(group));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<ApiResponse<GroupResponse>> getGroupWithMembers(@PathVariable Long id) {
        GroupResponse group = groupService.getGroupWithMembers(id);
        return ResponseEntity.ok(ApiResponse.success(group));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<GroupResponse>>> getUserGroups(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<GroupResponse> groups = groupService.getUserGroups(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(groups));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<GroupResponse>>> searchGroups(@RequestParam String keyword) {
        List<GroupResponse> groups = groupService.searchGroups(keyword);
        return ResponseEntity.ok(ApiResponse.success("搜索群组成功", groups));
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<ApiResponse<Void>> joinGroup(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        groupService.joinGroup(userDetails.getUserId(), id);
        return ResponseEntity.ok(ApiResponse.success("加入群组成功"));
    }

    @PostMapping("/join")
    public ResponseEntity<ApiResponse<Void>> joinGroup(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody JoinGroupRequest request) {
        groupService.joinGroup(userDetails.getUserId(), request.getGroupId());
        return ResponseEntity.ok(ApiResponse.success("加入群组成功"));
    }

    @DeleteMapping("/{id}/leave")
    public ResponseEntity<ApiResponse<Void>> leaveGroup(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        groupService.leaveGroup(userDetails.getUserId(), id);
        return ResponseEntity.ok(ApiResponse.success("退出群组成功"));
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeFromGroup(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long groupId,
            @PathVariable Long userId) {
        groupService.removeFromGroup(userDetails.getUserId(), groupId, userId);
        return ResponseEntity.ok(ApiResponse.success("移除成员成功"));
    }

    @GetMapping("/{id}/members/list")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getGroupMembers(@PathVariable Long id) {
        List<UserResponse> members = groupService.getGroupMembers(id);
        return ResponseEntity.ok(ApiResponse.success(members));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteGroup(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        groupService.deleteGroup(userDetails.getUserId(), id);
        return ResponseEntity.ok(ApiResponse.success("解散群组成功"));
    }
}