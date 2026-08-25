package com.example.loginauth.user;

import com.example.loginauth.auth.UsernameRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsernameRules usernameRules;
    private final String username;
    private final String password;

    public AdminInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            UsernameRules usernameRules,
            @Value("${ADMIN_USERNAME:}") String username,
            @Value("${ADMIN_PASSWORD:}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.usernameRules = usernameRules;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (username.isBlank() || password.isBlank()) {
            log.info("Admin bootstrap skipped because ADMIN_USERNAME or ADMIN_PASSWORD is not set");
            return;
        }
        String normalized = usernameRules.normalizeTrusted(username);
        if (userRepository.existsByUsername(normalized)) {
            log.info("Admin bootstrap skipped because the account already exists");
            return;
        }
        userRepository.save(new User(normalized, passwordEncoder.encode(password), UserRole.ADMIN));
        log.info("Admin bootstrap account created");
    }
}
