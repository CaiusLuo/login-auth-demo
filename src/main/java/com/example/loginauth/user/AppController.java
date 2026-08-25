package com.example.loginauth.user;

import com.example.loginauth.auth.UserResponse;
import com.example.loginauth.common.ApiException;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app")
public class AppController {

    private final UserRepository userRepository;

    public AppController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    AppResponse app(Principal principal) {
        UserResponse user = userRepository.findByUsername(principal.getName())
                .map(UserResponse::from)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Authentication required"));
        return new AppResponse(user, "Resource A", "This is mock content available to every signed-in user.");
    }

    record AppResponse(UserResponse user, String resource, String content) {
    }
}
