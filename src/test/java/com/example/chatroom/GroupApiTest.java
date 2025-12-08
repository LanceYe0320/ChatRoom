package com.example.chatroom;

import com.example.chatroom.dto.request.CreateGroupRequest;
import com.example.chatroom.dto.response.ApiResponse;
import com.example.chatroom.dto.response.GroupResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

@SpringBootTest
public class GroupApiTest {

    @Test
    public void testCreateGroup() {
        // 这只是一个示例测试，实际测试需要有效的JWT token
        System.out.println("Group API测试");
    }
}