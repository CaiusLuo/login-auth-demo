package com.example.loginauth.auth;

import com.example.loginauth.user.User;
import java.time.Instant;

public record UserResponse(Long id, String username, String role, boolean enabled, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(), user.getUsername(), user.getRole().name(), user.isEnabled(), user.getCreatedAt());
    }
}
