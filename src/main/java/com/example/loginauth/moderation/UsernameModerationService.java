package com.example.loginauth.moderation;

import com.example.loginauth.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class UsernameModerationService {

    private final UsernameModerationClient client;
    private final UsernameReviewRepository reviewRepository;

    public UsernameModerationService(
            UsernameModerationClient client,
            UsernameReviewRepository reviewRepository) {
        this.client = client;
        this.reviewRepository = reviewRepository;
    }

    public UsernameReviewResult review(String username) {
        UsernameReviewResult result;
        try {
            result = client.review(username);
            validate(result);
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw unavailable();
        }
        reviewRepository.save(new UsernameReview(
                username, result.decision().name(), result.reasonCode(), client.modelName()));
        return result;
    }

    private void validate(UsernameReviewResult result) {
        if (result == null || result.decision() == null || isBlank(result.reasonCode())) {
            throw unavailable();
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private ApiException unavailable() {
        return new ApiException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "MODERATION_UNAVAILABLE",
                "Username moderation is temporarily unavailable");
    }
}
