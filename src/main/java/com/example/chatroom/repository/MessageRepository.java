package com.example.chatroom.repository;

import com.example.chatroom.entity.Message;
import com.example.chatroom.entity.enums.MessageStatus;
import com.example.chatroom.entity.enums.MessageType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // 获取两个用户之间的私聊消息
    @Query("SELECT m FROM Message m WHERE m.messageType = 'PRIVATE' " +
            "AND ((m.sender.id = :userId1 AND m.receiver.id = :userId2) " +
            "OR (m.sender.id = :userId2 AND m.receiver.id = :userId1)) " +
            "ORDER BY m.createdAt DESC")
    Page<Message> findPrivateMessages(@Param("userId1") Long userId1,
                                      @Param("userId2") Long userId2,
                                      Pageable pageable);

    // 获取群组消息
    @Query("SELECT m FROM Message m WHERE m.messageType = 'GROUP' " +
            "AND m.group.id = :groupId ORDER BY m.createdAt DESC")
    Page<Message> findGroupMessages(@Param("groupId") Long groupId, Pageable pageable);

    // 获取用户的离线私聊消息
    @Query("SELECT m FROM Message m WHERE m.messageType = 'PRIVATE' " +
            "AND m.receiver.id = :userId AND m.status = 'SENT' " +
            "AND m.createdAt > :lastLogoutTime ORDER BY m.createdAt ASC")
    List<Message> findOfflinePrivateMessages(@Param("userId") Long userId,
                                             @Param("lastLogoutTime") LocalDateTime lastLogoutTime);

    // 获取用户的离线私聊消息（改进版，包含未送达的消息）
    @Query("SELECT m FROM Message m WHERE m.messageType = 'PRIVATE' " +
            "AND m.receiver.id = :userId AND m.createdAt > :lastLoginTime " +
            "ORDER BY m.createdAt ASC")
    List<Message> findUnreadPrivateMessagesForUser(@Param("userId") Long userId,
                                                   @Param("lastLoginTime") LocalDateTime lastLoginTime);

    // 获取用户未读的私聊消息
    @Query("SELECT m FROM Message m WHERE m.messageType = 'PRIVATE' " +
            "AND m.receiver.id = :userId AND m.status != 'READ' " +
            "ORDER BY m.createdAt ASC")
    List<Message> findUnreadPrivateMessages(@Param("userId") Long userId);

    // 获取用户在某个群组中的未读消息
    @Query("SELECT m FROM Message m WHERE m.messageType = 'GROUP' " +
            "AND m.group.id = :groupId AND m.createdAt > :lastReadTime " +
            "ORDER BY m.createdAt ASC")
    List<Message> findUnreadGroupMessages(@Param("groupId") Long groupId,
                                          @Param("lastReadTime") LocalDateTime lastReadTime);

    // 更新消息状态
    @Modifying
    @Query("UPDATE Message m SET m.status = :status, m.readAt = :readAt " +
            "WHERE m.id = :messageId")
    void updateMessageStatus(@Param("messageId") Long messageId,
                             @Param("status") MessageStatus status,
                             @Param("readAt") LocalDateTime readAt);

    // 批量将消息标记为已读
    @Modifying
    @Query("UPDATE Message m SET m.status = 'READ', m.readAt = :readAt " +
            "WHERE m.receiver.id = :receiverId AND m.sender.id = :senderId " +
            "AND m.status != 'READ'")
    void markMessagesAsRead(@Param("receiverId") Long receiverId,
                            @Param("senderId") Long senderId,
                            @Param("readAt") LocalDateTime readAt);

    // 获取用户的最近会话列表
    @Query("SELECT DISTINCT CASE " +
            "WHEN m.sender.id = :userId THEN m.receiver.id " +
            "ELSE m.sender.id END " +
            "FROM Message m WHERE m.messageType = 'PRIVATE' " +
            "AND (m.sender.id = :userId OR m.receiver.id = :userId)")
    List<Long> findRecentConversationUserIds(@Param("userId") Long userId);
}