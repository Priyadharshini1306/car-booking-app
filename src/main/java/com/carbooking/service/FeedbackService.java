package com.carbooking.service;

import com.carbooking.entity.*;
import com.carbooking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackService {

    private final FeedbackRepository
            feedbackRepo;
    private final BookingRepository
            bookingRepo;
    private final UserRepository userRepo;
    private final EmailService emailService;

    @Value("${admin.email}")
    private String adminEmail;

    /**
     * Submit feedback
     * If rating <= 2 (low rating)
     * → immediately notify admin by email
     */
    public Feedback submitFeedback(
            Long bookingId,
            Integer rating,
            String comment,
            String email) {

        // Validate rating
        if (rating < 1 || rating > 5) {
            throw new RuntimeException(
                    "Rating must be between 1 and 5");
        }

        User user = userRepo
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found"));

        Booking booking = bookingRepo
                .findById(bookingId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Booking not found"));

        // Build feedback
        Feedback feedback = Feedback.builder()
                .booking(booking)
                .user(user)
                .rating(rating)
                .comment(comment)
                .isNotifiedAdmin(false)
                .build();

        Feedback saved =
                feedbackRepo.save(feedback);

        log.info(
                "Feedback submitted by: {} " +
                        "- Rating: {}",
                user.getName(), rating);

        // ── LOW RATING ALERT ──
        // Immediately notify admin if
        // rating is 1 or 2 (bad feedback)
        if (rating <= 2) {
            notifyAdminBadFeedback(
                    saved, user, booking, rating,
                    comment);
        }

        return saved;
    }

    /**
     * Send immediate email to admin
     * when bad feedback is received
     */
    private void notifyAdminBadFeedback(
            Feedback feedback,
            User user,
            Booking booking,
            Integer rating,
            String comment) {

        String stars = getStars(rating);
        String carName = booking.getCar() != null
                ? booking.getCar().getName() : "N/A";
        String carBrand =
                booking.getCar() != null &&
                        booking.getCar().getBrand() != null
                        ? booking.getCar().getBrand() : "";

        String subject =
                "🚨 BAD FEEDBACK ALERT - " +
                        rating + "/5 Stars - " + carName;

        String body =
                "Dear Admin,\n\n" +
                        "A customer has given a LOW rating!\n\n"+
                        "=============================\n" +
                        "FEEDBACK DETAILS\n" +
                        "=============================\n" +
                        "Customer : " + user.getName() + "\n" +
                        "Email    : " + user.getEmail() + "\n" +
                        "Phone    : " +
                        (user.getPhone() != null
                                ? user.getPhone() : "N/A") + "\n\n"+
                        "Booking ID : #" +
                        booking.getId() + "\n" +
                        "Car        : " + carName +
                        " " + carBrand + "\n" +
                        "Booking Date: " +
                        booking.getStartDatetime()
                                .toLocalDate() + "\n\n" +
                        "Rating  : " + stars +
                        " (" + rating + "/5)\n" +
                        "Comment : " +
                        (comment != null &&
                                !comment.isEmpty()
                                ? comment
                                : "No comment provided") + "\n\n" +
                        "=============================\n" +
                        "Please follow up with the customer\n" +
                        "to resolve any issues.\n\n" +
                        "DriveEasy System";

        emailService.sendEmail(
                adminEmail, subject, body);

        // Mark as notified
        feedback.setIsNotifiedAdmin(true);
        feedbackRepo.save(feedback);

        log.warn(
                "BAD FEEDBACK ALERT sent to admin! " +
                        "User: {} - Rating: {}/5",
                user.getName(), rating);
    }

    // Get all feedback
    public List<Feedback> getAllFeedback() {
        return feedbackRepo
                .findAllByOrderByCreatedAtDesc();
    }

    // Get feedback by booking
    public List<Feedback> getByBooking(
            Long bookingId) {
        return feedbackRepo.findAll()
                .stream()
                .filter(f ->
                        f.getBooking().getId()
                                .equals(bookingId))
                .toList();
    }

    // Star rating display
    private String getStars(int rating) {
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < rating; i++) {
            stars.append("★");
        }
        for (int i = rating; i < 5; i++) {
            stars.append("☆");
        }
        return stars.toString();
    }
}