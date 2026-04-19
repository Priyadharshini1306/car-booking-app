package com.carbooking.repository;

import com.carbooking.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ChatMessageRepository
        extends JpaRepository<ChatMessage, Long> {

    // Get all messages by sender
    List<ChatMessage> findBySenderIdOrderByCreatedAtAsc(
            Long senderId);

    // Get unread messages from users
    List<ChatMessage> findByIsFromAdminFalseAndIsReadFalse();

    // Get all messages for a user
    // (sent by user OR sent to user by admin)
    @Query("SELECT m FROM ChatMessage m WHERE " +
            "(m.sender.id = :userId AND " +
            "m.isFromAdmin = false) OR " +
            "(m.receiverId = :userId AND " +
            "m.isFromAdmin = true) " +
            "ORDER BY m.createdAt ASC")
    List<ChatMessage> findConversation(
            @Param("userId") Long userId);

    // Get all unique users who sent messages
    @Query("SELECT DISTINCT m.sender FROM " +
            "ChatMessage m WHERE " +
            "m.isFromAdmin = false")
    List<com.carbooking.entity.User>
    findUsersWithMessages();
}