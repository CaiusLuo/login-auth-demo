package com.example.loginauth.moderation;

public interface UsernameModerationClient {

    UsernameReviewResult review(String username);

    default String modelName() {
        return "test-or-local";
    }
}
