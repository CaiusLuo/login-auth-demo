package com.example.loginauth.auth;

import com.example.loginauth.common.ApiException;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class UsernameRules {

    private static final Pattern ALLOWED = Pattern.compile("^[\\p{L}\\p{N}_-]+$");
    private static final Set<String> RESERVED = Set.of("admin", "root", "system", "moderator", "support");

    public String normalizeAndValidate(String input) {
        String username = Normalizer.normalize(input.trim(), Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        int length = username.codePointCount(0, username.length());
        if (length < 3 || length > 32) {
            throw rejected("Username must contain between 3 and 32 characters");
        }
        if (!ALLOWED.matcher(username).matches()) {
            throw rejected("Username contains unsupported characters");
        }
        if (RESERVED.contains(username)) {
            throw rejected("Username is reserved");
        }
        return username;
    }

    public String normalizeTrusted(String input) {
        return Normalizer.normalize(input.trim(), Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
    }

    private ApiException rejected(String message) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "USERNAME_REJECTED", message);
    }
}
