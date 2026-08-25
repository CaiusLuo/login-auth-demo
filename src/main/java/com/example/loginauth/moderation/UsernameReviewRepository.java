package com.example.loginauth.moderation;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UsernameReviewRepository extends JpaRepository<UsernameReview, Long> {
}
