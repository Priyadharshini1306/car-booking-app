package com.carbooking.controller;

import com.carbooking.entity.Booking;
import com.carbooking.entity.Driver;
import com.carbooking.repository.BookingRepository;
import com.carbooking.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/driver")
@RequiredArgsConstructor
public class DriverController {

    private final BookingRepository bookingRepo;
    private final DriverRepository driverRepo;

    // Verify driver login
    @GetMapping("/verify")
    public ResponseEntity<?> verifyDriver(
            @RequestParam Long driverId,
            @RequestParam String phone) {
        return driverRepo.findById(driverId)
                .map(driver -> {
                    if (!driver.getPhone().equals(phone))
                        return ResponseEntity.status(401)
                                .body(Map.of("message",
                                        "Invalid phone number"));
                    if (Boolean.TRUE.equals(
                            driver.getIsBlocked()))
                        return ResponseEntity.status(403)
                                .body(Map.of("message",
                                        "Account blocked"));
                    return ResponseEntity.ok(driver);
                })
                .orElse(ResponseEntity.status(404)
                        .body(Map.of("message",
                                "Driver not found")));
    }

    // Get bookings for driver
    @GetMapping("/{driverId}/bookings")
    public List<Booking> getDriverBookings(
            @PathVariable Long driverId) {
        return bookingRepo.findAll().stream()
                .filter(b ->
                        b.getDriver() != null &&
                                b.getDriver().getId()
                                        .equals(driverId))
                .toList();
    }

    // Driver updates payment status
    @PutMapping("/bookings/{id}/payment")
    public ResponseEntity<?> updatePayment(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        Booking booking = bookingRepo
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Booking not found"));
        String ps = body.get("paymentStatus");
        if (ps != null) {
            booking.setPaymentStatus(
                    Booking.PaymentStatus.valueOf(ps));
            if ("PAID".equals(ps))
                booking.setStatus(
                        Booking.BookingStatus.CONFIRMED);
            bookingRepo.save(booking);
        }
        return ResponseEntity.ok(booking);
    }

    // Driver updates trip status
    @PutMapping("/bookings/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        Booking booking = bookingRepo
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Booking not found"));
        String status = body.get("status");
        if (status != null) {
            booking.setStatus(
                    Booking.BookingStatus.valueOf(status));
            bookingRepo.save(booking);
        }
        return ResponseEntity.ok(booking);
    }

    /**
     * Driver applies for leave
     * Leave is based on DATE not approval
     * Driver shows unavailable on leave dates
     * automatically
     */
    @PutMapping("/{driverId}/apply-leave")
    public ResponseEntity<?> applyLeave(
            @PathVariable Long driverId,
            @RequestBody Map<String, String> body) {
        Driver driver = driverRepo
                .findById(driverId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Driver not found"));

        if (driver.getLeaveStatus() ==
                Driver.LeaveStatus.PENDING)
            return ResponseEntity.badRequest()
                    .body(Map.of("message",
                            "You already have a " +
                                    "pending leave request"));

        LocalDate leaveStart =
                LocalDate.parse(body.get("leaveStart"));
        LocalDate leaveEnd =
                LocalDate.parse(body.get("leaveEnd"));

        // Save leave dates directly
        // Driver will be unavailable on
        // these dates automatically
        driver.setRequestedLeaveStart(leaveStart);
        driver.setRequestedLeaveEnd(leaveEnd);
        driver.setLeaveReason(body.get("reason"));
        driver.setLeaveStatus(
                Driver.LeaveStatus.PENDING);

        // Also set actual leave dates
        // so system blocks on those dates
        driver.setLeaveStart(leaveStart);
        driver.setLeaveEnd(leaveEnd);

        driverRepo.save(driver);

        return ResponseEntity.ok(
                Map.of("message",
                        "Leave request submitted! " +
                                "You will be shown as unavailable " +
                                "from " + leaveStart +
                                " to " + leaveEnd));
    }

    // Get driver leave status
    @GetMapping("/{driverId}/leave-status")
    public ResponseEntity<?> leaveStatus(
            @PathVariable Long driverId) {
        Driver driver = driverRepo
                .findById(driverId)
                .orElseThrow();
        return ResponseEntity.ok(Map.of(
                "leaveStatus",
                driver.getLeaveStatus() != null
                        ? driver.getLeaveStatus().toString()
                        : "NONE",
                "leaveStart",
                driver.getRequestedLeaveStart() != null
                        ? driver.getRequestedLeaveStart()
                        .toString() : "",
                "leaveEnd",
                driver.getRequestedLeaveEnd() != null
                        ? driver.getRequestedLeaveEnd()
                        .toString() : "",
                "reason",
                driver.getLeaveReason() != null
                        ? driver.getLeaveReason() : "",
                "onLeaveToday",
                driver.isOnLeaveToday()
        ));
    }

    // Cancel leave request
    @PutMapping("/{driverId}/cancel-leave")
    public ResponseEntity<?> cancelLeave(
            @PathVariable Long driverId) {
        Driver driver = driverRepo
                .findById(driverId).orElseThrow();
        driver.setLeaveStatus(
                Driver.LeaveStatus.NONE);
        driver.setRequestedLeaveStart(null);
        driver.setRequestedLeaveEnd(null);
        driver.setLeaveReason(null);
        driver.setLeaveStart(null);
        driver.setLeaveEnd(null);
        driver.setOnLeave(false);
        driverRepo.save(driver);
        return ResponseEntity.ok(
                Map.of("message",
                        "Leave request cancelled"));
    }
}