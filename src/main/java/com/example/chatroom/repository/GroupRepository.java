package com.example.chatroom.repository;
import com.example.chatroom.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    Optional<Group> findByName(String name);

    boolean existsByName(String name);

    @Query("SELECT g FROM Group g JOIN g.members m WHERE m.user.id = :userId")
    List<Group> findGroupsByUserId(@Param("userId") Long userId);

    @Query("SELECT g FROM Group g WHERE g.owner.id = :ownerId")
    List<Group> findByOwnerId(@Param("ownerId") Long ownerId);

    @Query("SELECT g FROM Group g WHERE g.name LIKE %:keyword% OR g.description LIKE %:keyword%")
    List<Group> searchGroups(@Param("keyword") String keyword);

    @Query("SELECT g FROM Group g LEFT JOIN FETCH g.members WHERE g.id = :groupId")
    Optional<Group> findByIdWithMembers(@Param("groupId") Long groupId);
}
