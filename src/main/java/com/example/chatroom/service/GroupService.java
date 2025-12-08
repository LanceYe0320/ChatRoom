package com.example.chatroom.service;

import com.example.chatroom.dto.request.CreateGroupRequest;
import com.example.chatroom.dto.response.GroupResponse;
import com.example.chatroom.dto.response.UserResponse;

import java.util.List;

public interface GroupService {

    GroupResponse createGroup(Long ownerId, CreateGroupRequest request);

    GroupResponse getGroupById(Long groupId);

    GroupResponse getGroupWithMembers(Long groupId);

    List<GroupResponse> getUserGroups(Long userId);

    List<GroupResponse> searchGroups(String keyword);

    void joinGroup(Long userId, Long groupId);

    void leaveGroup(Long userId, Long groupId);

    void removeFromGroup(Long ownerId, Long groupId, Long userId);

    List<UserResponse> getGroupMembers(Long groupId);

    void deleteGroup(Long ownerId, Long groupId);

    GroupResponse updateGroup(Long ownerId, Long groupId, String name, String description, String avatar);
}