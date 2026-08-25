package com.example.loginauth.auth;

import com.example.loginauth.common.ApiException;
import com.example.loginauth.moderation.ModerationDecision;
import com.example.loginauth.moderation.UsernameModerationService;
import com.example.loginauth.user.User;
import com.example.loginauth.user.UserRepository;
import com.example.loginauth.user.UserRole;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class RegistrationService {

    private final UsernameRules usernameRules;
    private final UserRepository userRepository;
    private final UsernameModerationService moderationService;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transactionTemplate;

    public RegistrationService(
            UsernameRules usernameRules,
            UserRepository userRepository,
            UsernameModerationService moderationService,
            PasswordEncoder passwordEncoder,
            TransactionTemplate transactionTemplate) {
        this.usernameRules = usernameRules;
        this.userRepository = userRepository;
        this.moderationService = moderationService;
        this.passwordEncoder = passwordEncoder;
        this.transactionTemplate = transactionTemplate;
    }

    public User register(RegisterRequest request) {
        String username = usernameRules.normalizeAndValidate(request.username());
        if (userRepository.existsByUsername(username)) {
            throw duplicate();
        }

        var review = moderationService.review(username);
        if (review.decision() != ModerationDecision.ALLOW) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "USERNAME_REJECTED",
                    review.decision() == ModerationDecision.REVIEW
                            ? "Username requires review; choose another username"
                            : "Username does not comply with the community policy");
        }

        String passwordHash = passwordEncoder.encode(request.password());
        try {
            return transactionTemplate.execute(status ->
                    userRepository.saveAndFlush(new User(username, passwordHash, UserRole.USER)));
        } catch (DataIntegrityViolationException exception) {
            throw duplicate();
        }
    }

    private ApiException duplicate() {
        return new ApiException(HttpStatus.CONFLICT, "DUPLICATE_USERNAME", "Username already exists");
    }
}
