package com.example.chatroom.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMessageRequest {

    @NotNull(message = "群组ID不能为空")
    private Long groupId;

    @NotBlank(message = "消息内容不能为空")
    private String content;
}