package com.example.chatroom.service.impl;

import com.example.chatroom.dto.request.CreateGroupRequest;
import com.example.chatroom.dto.response.GroupResponse;
import com.example.chatroom.dto.response.UserResponse;
import com.example.chatroom.entity.Group;
import com.example.chatroom.entity.GroupMember;
import com.example.chatroom.entity.User;
import com.example.chatroom.exception.BadRequestException;
import com.example.chatroom.exception.ResourceNotFoundException;
import com.example.chatroom.exception.UnauthorizedException;
import com.example.chatroom.repository.GroupMemberRepository;
import com.example.chatroom.repository.GroupRepository;
import com.example.chatroom.repository.UserRepository;
import com.example.chatroom.service.GroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public GroupResponse createGroup(Long ownerId, CreateGroupRequest request) {
        // 检查群名是否已存在
        if (groupRepository.existsByName(request.getName())) {
            throw new BadRequestException("群组名称已存在");
        }

        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        // 创建群组
        Group group = Group.builder()
                .name(request.getName())
                .description(request.getDescription())
                .avatar(request.getAvatar())
                .owner(owner)
                .maxMembers(request.getMaxMembers() != null ? request.getMaxMembers() : 200)
                .build();

        group = groupRepository.save(group);

        // 将创建者加入群组
        GroupMember ownerMember = GroupMember.builder()
                .group(group)
                .user(owner)
                .role("OWNER")
                .build();
        groupMemberRepository.save(ownerMember);

        log.info("群组创建成功: {} by {}", group.getName(), owner.getUsername());
        return GroupResponse.fromEntity(group);
    }

    @Override
    @Transactional(readOnly = true)
    public GroupResponse getGroupById(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("群组不存在"));
        return GroupResponse.fromEntity(group);
    }

    @Override
    @Transactional(readOnly = true)
    public GroupResponse getGroupWithMembers(Long groupId) {
        Group group = groupRepository.findByIdWithMembers(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("群组不存在"));

        List<UserResponse> members = group.getMembers().stream()
                .map(gm -> UserResponse.fromEntity(gm.getUser()))
                .collect(Collectors.toList());

        return GroupResponse.fromEntityWithMembers(group, members);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupResponse> getUserGroups(Long userId) {
        return groupRepository.findGroupsByUserId(userId)
                .stream()
                .map(GroupResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupResponse> searchGroups(String keyword) {
        return groupRepository.searchGroups(keyword)
                .stream()
                .map(GroupResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void joinGroup(Long userId, Long groupId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("群组不存在"));

        // 检查是否已经是成员
        if (groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new BadRequestException("您已经是该群组成员");
        }

        // 检查群组人数
        int currentMembers = groupMemberRepository.countByGroupId(groupId);
        if (currentMembers >= group.getMaxMembers()) {
            throw new BadRequestException("群组成员已满");
        }

        GroupMember member = GroupMember.builder()
                .group(group)
                .user(user)
                .role("MEMBER")
                .build();
        groupMemberRepository.save(member);

        log.info("用户 {} 加入群组 {}", user.getUsername(), group.getName());
    }

    @Override
    @Transactional
    public void leaveGroup(Long userId, Long groupId) {
        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new BadRequestException("您不是该群组成员"));

        // 群主不能直接离开
        if ("OWNER".equals(member.getRole())) {
            throw new BadRequestException("群主不能离开群组，请先转让群主或解散群组");
        }

        groupMemberRepository.delete(member);
        log.info("用户 {} 离开群组 {}", userId, groupId);
    }

    @Override
    @Transactional
    public void removeFromGroup(Long ownerId, Long groupId, Long userId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("群组不存在"));

        // 检查操作者是否是群主
        if (!group.getOwner().getId().equals(ownerId)) {
            throw new UnauthorizedException("只有群主才能踢出成员");
        }

        // 不能踢出自己
        if (ownerId.equals(userId)) {
            throw new BadRequestException("不能将自己踢出群组");
        }

        groupMemberRepository.deleteByGroupIdAndUserId(groupId, userId);
        log.info("用户 {} 被踢出群组 {}", userId, groupId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getGroupMembers(Long groupId) {
        return groupMemberRepository.findByGroupIdWithUser(groupId)
                .stream()
                .map(gm -> UserResponse.fromEntity(gm.getUser()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteGroup(Long ownerId, Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("群组不存在"));

        if (!group.getOwner().getId().equals(ownerId)) {
            throw new UnauthorizedException("只有群主才能解散群组");
        }

        groupRepository.delete(group);
        log.info("群组 {} 已被解散", group.getName());
    }

    @Override
    @Transactional
    public GroupResponse updateGroup(Long ownerId, Long groupId, String name, String description, String avatar) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("群组不存在"));

        if (!group.getOwner().getId().equals(ownerId)) {
            throw new UnauthorizedException("只有群主才能修改群组信息");
        }

        if (name != null && !name.trim().isEmpty()) {
            // 检查新名称是否被占用
            if (!group.getName().equals(name) && groupRepository.existsByName(name)) {
                throw new BadRequestException("群组名称已存在");
            }
            group.setName(name);
        }
        if (description != null) {
            group.setDescription(description);
        }
        if (avatar != null) {
            group.setAvatar(avatar);
        }

        group = groupRepository.save(group);
        return GroupResponse.fromEntity(group);
    }
}