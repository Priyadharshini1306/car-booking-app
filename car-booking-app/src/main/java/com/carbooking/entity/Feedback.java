package com.carbooking.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "feedback")
@Data @NoArgsConstructor
@AllArgsConstructor @Builder
public class Feedback {

    @Id
    @GeneratedValue(strategy =
            GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "booking_id",
            nullable = false)
    @JsonIgnoreProperties({
            "user", "hibernateLazyInitializer"})
    private Booking booking;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id",
            nullable = false)
    @JsonIgnoreProperties({
            "password", "hibernateLazyInitializer"})
    private User user;

    @Column(nullable = false)
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    private Boolean isNotifiedAdmin = false;

    private LocalDateTime createdAt =
            LocalDateTime.now();
}
