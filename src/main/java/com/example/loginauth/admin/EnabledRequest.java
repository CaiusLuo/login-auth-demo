package com.example.loginauth.admin;

import jakarta.validation.constraints.NotNull;

public record EnabledRequest(@NotNull Boolean enabled) {
}
