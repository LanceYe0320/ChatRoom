package com.example.chatroom.service;

import com.example.chatroom.dto.request.LoginRequest;
import com.example.chatroom.dto.request.RegisterRequest;
import com.example.chatroom.dto.response.JwtResponse;
import com.example.chatroom.dto.response.UserResponse;
import com.example.chatroom.entity.User;

import java.util.List;

public interface UserService {

    JwtResponse register(RegisterRequest request);

    JwtResponse login(LoginRequest request);

    UserResponse getCurrentUser(Long userId);

    UserResponse getUserById(Long userId);

    UserResponse getUserByUsername(String username);

    List<UserResponse> getOnlineUsers();

    List<UserResponse> searchUsers(String keyword);

    List<UserResponse> getAllUsers();

    UserResponse updateProfile(Long userId, String nickname, String avatar);

    void changePassword(Long userId, String oldPassword, String newPassword);
}
