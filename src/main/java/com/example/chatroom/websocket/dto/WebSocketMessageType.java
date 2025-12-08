package com.example.chatroom.websocket.dto;

public enum WebSocketMessageType {
    // 连接相关
    CONNECT,
    DISCONNECT,

    // 私聊消息
    PRIVATE_MESSAGE,
    PRIVATE_MESSAGE_ACK,

    // 群聊消息
    GROUP_MESSAGE,
    GROUP_MESSAGE_ACK,

    // 群组相关
    JOIN_GROUP,
    LEAVE_GROUP,
    GROUP_NOTIFICATION,

    // 用户状态
    USER_ONLINE,
    USER_OFFLINE,

    // 系统消息
    SYSTEM,
    ERROR,

    // 心跳
    PING,
    PONG
}