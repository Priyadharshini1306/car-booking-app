package com.carbooking.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name = "cars")
@Data @NoArgsConstructor
@AllArgsConstructor @Builder
@JsonIgnoreProperties({
        "hibernateLazyInitializer", "handler"})
public class Car {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;
    private String brand;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CarType type;

    @Column(nullable = false)
    private Integer seats;

    private String fuelType;
    private String transmission;
    private String imageUrl;

    @Column(nullable = false)
    private BigDecimal rentPerHour;

    @Column(nullable = false)
    private BigDecimal rentPerDay;

    private LocalDate rcExpiry;
    private LocalDate fcExpiry;
    private LocalDate insuranceExpiry;

    private Boolean isAvailable = true;
    private Boolean isBlocked = false;

    // ── Permanent Morning Driver ──
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "morning_driver_id")
    @JsonIgnoreProperties({
            "hibernateLazyInitializer", "handler"})
    private Driver morningDriver;

    // ── Permanent Evening Driver ──
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "evening_driver_id")
    @JsonIgnoreProperties({
            "hibernateLazyInitializer", "handler"})
    private Driver eveningDriver;

    // ── Permanent Night Driver ──
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "night_driver_id")
    @JsonIgnoreProperties({
            "hibernateLazyInitializer", "handler"})
    private Driver nightDriver;

    @Column(name = "created_at")
    private LocalDateTime createdAt =
            LocalDateTime.now();

    public enum CarType {
        LUXURY, COMFORT, NORMAL
    }
}