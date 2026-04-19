package com.carbooking.entity;

import com.fasterxml.jackson.annotation
        .JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "chat_messages")
@Data @NoArgsConstructor
@AllArgsConstructor @Builder
public class ChatMessage {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sender_id",
            nullable = false)
    @JsonIgnoreProperties({
            "password",
            "hibernateLazyInitializer",
            "handler"
    })
    private User sender;

    private Long receiverId;

    @Column(nullable = false,
            columnDefinition = "TEXT")
    private String message;

    private Boolean isFromAdmin = false;
    private Boolean isRead = false;

    private LocalDateTime createdAt =
            LocalDateTime.now();
}