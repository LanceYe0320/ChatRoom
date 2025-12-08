package com.example.chatroom;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnectionTest {
    public static void main(String[] args) {
        // 测试不同的连接方式
        testConnection("jdbc:mysql://localhost:3306/mysql", "root", "root");
        testConnection("jdbc:mysql://127.0.0.1:3306/mysql", "root", "root");
        testConnection("jdbc:mysql://localhost:3306/chatroom", "chatroom_user", "chatroom_pass");
        testConnection("jdbc:mysql://127.0.0.1:3306/chatroom", "chatroom_user", "chatroom_pass");
    }
    
    private static void testConnection(String url, String username, String password) {
        System.out.println("\n尝试连接到: " + url + " 使用用户: " + username);
        
        try {
            // 设置连接属性
            Properties props = new Properties();
            props.setProperty("user", username);
            props.setProperty("password", password);
            props.setProperty("useSSL", "false");
            props.setProperty("serverTimezone", "UTC");
            props.setProperty("allowPublicKeyRetrieval", "true");
            
            Connection connection = DriverManager.getConnection(url, props);
            System.out.println("✓ 连接成功！");
            connection.close();
        } catch (SQLException e) {
            System.err.println("✗ 连接失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}