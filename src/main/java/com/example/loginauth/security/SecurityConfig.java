package com.example.loginauth.security;

import com.example.loginauth.common.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepository.setCookiePath("/");

        http
                .csrf(csrf -> csrf.csrfTokenRepository(csrfRepository))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/auth/**", "/actuator/health/**", "/", "/index.html",
                                "/assets/**", "/favicon.ico", "/login", "/register", "/app", "/admin")
                        .permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/app/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/**").denyAll()
                        .anyRequest().permitAll())
                .formLogin(form -> form
                        .loginProcessingUrl("/api/auth/login")
                        .successHandler((request, response, authentication) -> writeJson(
                                response, objectMapper, HttpServletResponse.SC_OK,
                                new LoginResult(authentication.getName())))
                        .failureHandler((request, response, exception) -> writeJson(
                                response, objectMapper, HttpServletResponse.SC_UNAUTHORIZED,
                                ApiError.of("INVALID_CREDENTIALS", "Invalid username or password"))))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((request, response, authentication) -> writeJson(
                                response, objectMapper, HttpServletResponse.SC_OK,
                                new MessageResult("Logged out"))))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> writeJson(
                                response, objectMapper, HttpServletResponse.SC_UNAUTHORIZED,
                                ApiError.of("UNAUTHENTICATED", "Authentication required")))
                        .accessDeniedHandler((request, response, exception) -> writeJson(
                                response, objectMapper, HttpServletResponse.SC_FORBIDDEN,
                                ApiError.of("FORBIDDEN", "Access denied"))))
                .sessionManagement(session -> session.sessionFixation(fixation -> fixation.changeSessionId()));

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private static void writeJson(HttpServletResponse response, ObjectMapper objectMapper, int status, Object body)
            throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    private record LoginResult(String username) {
    }

    private record MessageResult(String message) {
    }
}
