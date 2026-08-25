package com.example.loginauth.admin;

import com.example.loginauth.auth.UserResponse;
import com.example.loginauth.common.ApiException;
import com.example.loginauth.user.UserRepository;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;

    public AdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/users")
    List<UserResponse> users(@RequestParam(defaultValue = "") String search) {
        return userRepository.findTop50ByUsernameContainingIgnoreCaseOrderByUsername(search.trim()).stream()
                .map(UserResponse::from)
                .toList();
    }

    @PatchMapping("/users/{id}/enabled")
    @Transactional
    UserResponse updateEnabled(
            @PathVariable Long id,
            @Valid @RequestBody EnabledRequest request,
            Principal principal) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        if (user.getUsername().equals(principal.getName()) && !request.enabled()) {
            throw new ApiException(HttpStatus.CONFLICT, "SELF_DISABLE_FORBIDDEN", "Administrators cannot disable themselves");
        }
        user.setEnabled(request.enabled());
        return UserResponse.from(user);
    }
}
