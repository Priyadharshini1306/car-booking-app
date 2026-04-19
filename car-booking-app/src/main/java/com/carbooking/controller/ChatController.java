package com.carbooking.controller;

import com.carbooking.entity.*;
import com.carbooking.repository.*;
import com.carbooking.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // User sends message
    @PostMapping("/send")
    public ResponseEntity<ChatMessage> send(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal
            UserDetails userDetails) {
        ChatMessage msg =
                chatService.sendMessage(
                        userDetails.getUsername(),
                        body.get("message"));
        return ResponseEntity.ok(msg);
    }

    // User gets chat history
    @GetMapping("/history")
    public ResponseEntity<List<ChatMessage>>
    history(
            @AuthenticationPrincipal
            UserDetails userDetails) {
        return ResponseEntity.ok(
                chatService.getUserHistory(
                        userDetails.getUsername()));
    }

    // Admin: get users who chatted
    @GetMapping("/admin/users")
    public ResponseEntity<List<User>>
    getChatUsers() {
        return ResponseEntity.ok(
                chatService.getChatUsers());
    }

    // Admin: get conversation with user
    @GetMapping("/admin/user/{userId}")
    public ResponseEntity<List<ChatMessage>>
    getConversation(
            @PathVariable Long userId) {
        return ResponseEntity.ok(
                chatService.getConversation(userId));
    }

    // Admin: reply to user
    @PostMapping("/admin/reply")
    public ResponseEntity<ChatMessage>
    adminReply(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal
            UserDetails userDetails) {
        Long receiverId = Long.valueOf(
                body.get("receiverId").toString());
        String message =
                body.get("message").toString();
        ChatMessage msg =
                chatService.adminReply(
                        userDetails.getUsername(),
                        receiverId, message);
        return ResponseEntity.ok(msg);
    }

    // Admin: get all messages
    @GetMapping("/admin/all")
    public ResponseEntity<List<ChatMessage>>
    allMessages() {
        return ResponseEntity.ok(
                chatService.getConversation(0L));
    }

    // Admin: get unread
    @GetMapping("/admin/unread")
    public ResponseEntity<List<ChatMessage>>
    unread() {
        return ResponseEntity.ok(
                chatService.getUnread());
    }

    // Admin: mark as read
    @PutMapping("/admin/read/{userId}")
    public ResponseEntity<Void> markRead(
            @PathVariable Long userId) {
        chatService.markAsRead(userId);
        return ResponseEntity.ok().build();
    }
}