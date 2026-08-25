package com.example.loginauth.moderation;

public record UsernameReviewResult(
        ModerationDecision decision,
        String reasonCode,
        String reasonSummary) {
}
