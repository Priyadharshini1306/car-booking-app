package com.carbooking.repository;
import com.carbooking.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findByRatingLessThanEqual(Integer rating);
    List<Feedback> findAllByOrderByCreatedAtDesc();
}

