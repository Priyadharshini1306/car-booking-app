package com.carbooking.service;

import com.carbooking.entity.*;
import com.carbooking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatMessageRepository
            chatRepo;
    private final UserRepository userRepo;

    // User sends message to admin
    public ChatMessage sendMessage(
            String senderEmail, String message) {

        User sender = userRepo
                .findByEmail(senderEmail)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found"));

        if (message == null ||
                message.trim().isEmpty()) {
            throw new RuntimeException(
                    "Message cannot be empty");
        }

        ChatMessage msg = ChatMessage.builder()
                .sender(sender)
                .message(message.trim())
                .isFromAdmin(
                        sender.getRole() == User.Role.ADMIN)
                .isRead(false)
                .build();

        ChatMessage saved = chatRepo.save(msg);
        log.info("Message sent by: {} - {}",
                sender.getName(),
                sender.getRole());
        return saved;
    }

    // Admin replies to user
    public ChatMessage adminReply(
            String adminEmail,
            Long receiverId,
            String message) {

        User admin = userRepo
                .findByEmail(adminEmail)
                .orElseThrow();

        ChatMessage msg = ChatMessage.builder()
                .sender(admin)
                .receiverId(receiverId)
                .message(message.trim())
                .isFromAdmin(true)
                .isRead(false)
                .build();

        ChatMessage saved = chatRepo.save(msg);
        log.info(
                "Admin replied to user ID: {}",
                receiverId);
        return saved;
    }

    // Get user chat history
    public List<ChatMessage> getUserHistory(
            String email) {
        User user = userRepo
                .findByEmail(email)
                .orElseThrow();
        return chatRepo
                .findConversation(user.getId());
    }

    // Get all users who chatted
    public List<User> getChatUsers() {
        return chatRepo.findUsersWithMessages();
    }

    // Get conversation for admin
    public List<ChatMessage> getConversation(
            Long userId) {
        return chatRepo.findConversation(userId);
    }

    // Get unread messages
    public List<ChatMessage> getUnread() {
        return chatRepo
                .findByIsFromAdminFalseAndIsReadFalse();
    }

    // Mark messages as read
    public void markAsRead(Long userId) {
        List<ChatMessage> unread =
                chatRepo.findConversation(userId)
                        .stream()
                        .filter(m -> !m.getIsFromAdmin() &&
                                !m.getIsRead())
                        .toList();
        unread.forEach(m -> m.setIsRead(true));
        chatRepo.saveAll(unread);
        log.info("Marked {} messages as read",
                unread.size());
    }
}