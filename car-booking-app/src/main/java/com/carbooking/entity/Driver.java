package com.carbooking.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity @Table(name = "drivers")
@Data @NoArgsConstructor
@AllArgsConstructor @Builder
@JsonIgnoreProperties({
        "hibernateLazyInitializer", "handler"})
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;
    private String email;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private Integer age;

    @Column(unique = true, nullable = false)
    private String licenseNumber;

    @Column(nullable = false)
    private LocalDate licenseExpiry;

    @Enumerated(EnumType.STRING)
    private Shift shift = Shift.MORNING;

    // Overall availability flag (set by admin or scheduler)
    private Boolean isAvailable = true;
    private Boolean isBlocked   = false;

    // Leave tracking
    private Boolean   onLeave    = false;
    private LocalDate leaveStart;
    private LocalDate leaveEnd;

    // Leave request fields (pending approval)
    private LocalDate requestedLeaveStart;
    private LocalDate requestedLeaveEnd;
    private String    leaveReason;

    @Enumerated(EnumType.STRING)
    private LeaveStatus leaveStatus = LeaveStatus.NONE;

    // Service info
    private Integer    yearsOfService = 0;
    private BigDecimal salary;
    private LocalDate  joinedDate;

    public enum Shift {
        MORNING, EVENING, NIGHT
    }

    public enum LeaveStatus {
        NONE, PENDING, APPROVED, REJECTED
    }

    /**
     * Check if driver is on leave TODAY.
     *
     * Uses the actual leave date fields (leaveStart / leaveEnd)
     * so this is always accurate regardless of the DB flag.
     *
     * A driver is on leave today when:
     *   - leaveStart and leaveEnd are both set
     *   - today falls within [leaveStart, leaveEnd] inclusive
     */
    public boolean isOnLeaveToday() {
        LocalDate today = LocalDate.now();
        if (leaveStart == null || leaveEnd == null) return false;
        return !today.isBefore(leaveStart) && !today.isAfter(leaveEnd);
    }

    /**
     * FIX — isTrulyAvailable()
     *
     * ROOT CAUSE OF THE BUG:
     * Previously this method relied on the DB flag `isAvailable` which is
     * only reset by the 8AM scheduler. If the scheduler hadn't run yet
     * (app restart, first run after midnight, etc.) the driver would still
     * appear unavailable even though their leave ended.
     *
     * NEW LOGIC:
     * 1. Blocked drivers are never available (hard block by admin).
     * 2. If the driver is NOT on leave today (date-based check) AND their
     *    leave has actually ended (leaveEnd < today), treat them as available
     *    regardless of the stale isAvailable flag.
     * 3. If they are on leave today, they are not available.
     * 4. Otherwise fall back to the isAvailable flag (for admin-set unavailability).
     */
    public boolean isTrulyAvailable() {
        // Hard block always wins
        if (Boolean.TRUE.equals(isBlocked)) return false;

        LocalDate today = LocalDate.now();

        // Leave has ended — driver is available regardless of DB flag
        if (leaveEnd != null && leaveEnd.isBefore(today)) {
            return true;
        }

        // Currently on leave — not available
        if (isOnLeaveToday()) return false;

        // No leave active — respect the admin-set availability flag
        return Boolean.TRUE.equals(isAvailable);
    }
}