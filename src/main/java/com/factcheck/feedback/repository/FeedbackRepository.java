package com.factcheck.feedback.repository;

import com.factcheck.domain.UserFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<UserFeedback, Long> {
}
