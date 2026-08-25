package com.example.loginauth.moderation;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!prod")
public class LocalUsernameModerationClient implements UsernameModerationClient {

    @Override
    public UsernameReviewResult review(String username) {
        return new UsernameReviewResult(ModerationDecision.ALLOW, "LOCAL_ALLOW", "Local development decision");
    }

    @Override
    public String modelName() {
        return "local-development";
    }
}
