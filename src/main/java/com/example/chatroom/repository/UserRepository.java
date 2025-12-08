package com.example.chatroom.repository;

import com.example.chatroom.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<User> findByOnlineTrue();

    @Query("SELECT u FROM User u WHERE u.username LIKE %:keyword% OR u.nickname LIKE %:keyword%")
    List<User> searchUsers(@Param("keyword") String keyword);

    @Modifying
    @Query("UPDATE User u SET u.online = :online, u.lastLoginTime = :time WHERE u.id = :userId")
    void updateOnlineStatus(@Param("userId") Long userId,
                            @Param("online") Boolean online,
                            @Param("time") LocalDateTime time);

    @Modifying
    @Query("UPDATE User u SET u.online = false, u.lastLogoutTime = :time WHERE u.id = :userId")
    void setOffline(@Param("userId") Long userId, @Param("time") LocalDateTime time);
}