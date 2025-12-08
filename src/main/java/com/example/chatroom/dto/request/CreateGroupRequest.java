package com.example.chatroom.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGroupRequest {

    @NotBlank(message = "群组名称不能为空")
    @Size(min = 2, max = 100, message = "群组名称长度必须在2-100个字符之间")
    private String name;

    @Size(max = 500, message = "群组描述长度不能超过500个字符")
    private String description;

    private String avatar;

    private Integer maxMembers;
}
