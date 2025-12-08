package com.example.chatroom.dto.response;

import com.example.chatroom.entity.Group;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupResponse {

    private Long id;
    private String name;
    private String description;
    private String avatar;
    private Long ownerId;
    private String ownerUsername;
    private Integer maxMembers;
    private Integer currentMembers;
    private LocalDateTime createdAt;
    private List<UserResponse> members;

    public static GroupResponse fromEntity(Group group) {
        return GroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .avatar(group.getAvatar())
                .ownerId(group.getOwner().getId())
                .ownerUsername(group.getOwner().getUsername())
                .maxMembers(group.getMaxMembers())
                .currentMembers(group.getMembers().size())
                .createdAt(group.getCreatedAt())
                .build();
    }

    public static GroupResponse fromEntityWithMembers(Group group, List<UserResponse> members) {
        GroupResponse response = fromEntity(group);
        response.setMembers(members);
        return response;
    }
}
