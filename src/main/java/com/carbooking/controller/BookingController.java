package com.carbooking.controller;
import com.carbooking.dto.BookingRequest;
import com.carbooking.entity.Booking;
import com.carbooking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<Booking> createBooking(
            @Valid @RequestBody BookingRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        Booking booking = bookingService.createBooking(
                req, userDetails.getUsername());
        return ResponseEntity.ok(booking);
    }

    @GetMapping("/my")
    public List<Booking> myBookings(
            @AuthenticationPrincipal UserDetails userDetails) {
        return bookingService.getMyBookings(userDetails.getUsername());
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Booking> cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                bookingService.cancelBooking(id, userDetails.getUsername()));
    }
}

