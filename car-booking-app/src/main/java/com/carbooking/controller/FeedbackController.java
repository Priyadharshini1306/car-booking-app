package com.carbooking.controller;

import com.carbooking.entity.Feedback;
import com.carbooking.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService
            feedbackService;

    // Submit feedback
    @PostMapping
    public ResponseEntity<Feedback> submit(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal
            UserDetails userDetails) {

        Long bookingId = Long.valueOf(
                body.get("bookingId").toString());
        Integer rating = Integer.valueOf(
                body.get("rating").toString());
        String comment =
                body.get("comment") != null
                        ? body.get("comment").toString()
                        : "";

        Feedback saved =
                feedbackService.submitFeedback(
                        bookingId, rating, comment,
                        userDetails.getUsername());

        return ResponseEntity.ok(saved);
    }

    // Public: get all feedback
    @GetMapping("/public/all")
    public ResponseEntity<List<Feedback>>
    allFeedback() {
        return ResponseEntity.ok(
                feedbackService.getAllFeedback());
    }
}