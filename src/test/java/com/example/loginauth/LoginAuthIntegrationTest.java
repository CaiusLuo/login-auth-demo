package com.example.loginauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.loginauth.moderation.ModerationDecision;
import com.example.loginauth.moderation.UsernameModerationClient;
import com.example.loginauth.moderation.UsernameReviewRepository;
import com.example.loginauth.moderation.UsernameReviewResult;
import com.example.loginauth.user.User;
import com.example.loginauth.user.UserRepository;
import com.example.loginauth.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoginAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UsernameReviewRepository reviewRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UsernameModerationClient moderationClient;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        userRepository.deleteAll();
        reset(moderationClient);
        when(moderationClient.modelName()).thenReturn("test-model");
    }

    @Test
    void anonymousAppReturns401() throws Exception {
        mockMvc.perform(get("/api/app"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void anonymousAdminReturns401() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCanAccessAppButNotAdmin() throws Exception {
        createUser("alice", UserRole.USER, true);

        mockMvc.perform(get("/api/app").with(user("alice").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource").value("Resource A"));

        mockMvc.perform(get("/api/admin/users").with(user("alice").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessAppAndAdmin() throws Exception {
        createUser("admin", UserRole.ADMIN, true);

        mockMvc.perform(get("/api/app").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/users").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void allowCreatesUserRoleOnly() throws Exception {
        allowModeration();

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"Alice\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.role").value("USER"));

        assertThat(userRepository.findByUsername("alice")).get()
                .extracting(User::getRole)
                .isEqualTo(UserRole.USER);
    }

    @Test
    void registerCannotSubmitAdminRole() throws Exception {
        allowModeration();

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"password123\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isBadRequest());

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void duplicateReturns409WithoutCallingLlm() throws Exception {
        createUser("alice", UserRole.USER, true);

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"password123\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_USERNAME"));

        verify(moderationClient, never()).review(anyString());
    }

    @Test
    void localValidationReturns422WithoutCallingLlm() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"bad name!\",\"password\":\"password123\"}"))
                .andExpect(status().isUnprocessableEntity());

        verify(moderationClient, never()).review(anyString());
    }

    @Test
    void rejectDoesNotCreateUser() throws Exception {
        when(moderationClient.review("rejectedname"))
                .thenReturn(new UsernameReviewResult(ModerationDecision.REJECT, "ABUSE", "Rejected"));

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"rejectedname\",\"password\":\"password123\"}"))
                .andExpect(status().isUnprocessableEntity());

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void reviewDoesNotCreateUser() throws Exception {
        when(moderationClient.review("reviewname"))
                .thenReturn(new UsernameReviewResult(ModerationDecision.REVIEW, "UNCERTAIN", "Review"));

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"reviewname\",\"password\":\"password123\"}"))
                .andExpect(status().isUnprocessableEntity());

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void llmFailureReturns503WithoutCreatingUser() throws Exception {
        when(moderationClient.review("networkfail")).thenThrow(new RuntimeException("network"));

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"networkfail\",\"password\":\"password123\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("MODERATION_UNAVAILABLE"));

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void wrongPasswordReturns401() throws Exception {
        createUser("alice", UserRole.USER, true);

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "alice")
                        .param("password", "wrong-password"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void successfulLoginCreatesSessionForMeAndApp() throws Exception {
        createUser("alice", UserRole.USER, true);

        var login = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "alice")
                        .param("password", "password123"))
                .andExpect(status().isOk())
                .andReturn();

        var session = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session).isNotNull();
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));
        mockMvc.perform(get("/api/app").session(session))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/logout").session(session).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void invalidModerationResultReturns503WithoutCreatingUser() throws Exception {
        when(moderationClient.review("invalidresult")).thenReturn(null);

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"invalidresult\",\"password\":\"password123\"}"))
                .andExpect(status().isServiceUnavailable());

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void disabledUserCannotLogin() throws Exception {
        createUser("alice", UserRole.USER, false);

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "alice")
                        .param("password", "password123"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCannotModifyAdminApi() throws Exception {
        User target = createUser("target", UserRole.USER, true);

        mockMvc.perform(patch("/api/admin/users/{id}/enabled", target.getId())
                        .with(user("alice").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanModifyEnabled() throws Exception {
        createUser("admin", UserRole.ADMIN, true);
        User target = createUser("target", UserRole.USER, true);

        mockMvc.perform(patch("/api/admin/users/{id}/enabled", target.getId())
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        assertThat(userRepository.findById(target.getId())).get()
                .extracting(User::isEnabled)
                .isEqualTo(false);
    }

    @Test
    void adminCannotDisableSelf() throws Exception {
        User admin = createUser("admin", UserRole.ADMIN, true);

        mockMvc.perform(patch("/api/admin/users/{id}/enabled", admin.getId())
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SELF_DISABLE_FORBIDDEN"));
    }

    @Test
    void mutationWithoutCsrfIsRejected() throws Exception {
        User target = createUser("target", UserRole.USER, true);

        mockMvc.perform(patch("/api/admin/users/{id}/enabled", target.getId())
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isForbidden());
    }

    private void allowModeration() {
        when(moderationClient.review(anyString()))
                .thenReturn(new UsernameReviewResult(ModerationDecision.ALLOW, "OK", "Allowed"));
    }

    private User createUser(String username, UserRole role, boolean enabled) {
        User account = new User(username, passwordEncoder.encode("password123"), role);
        account.setEnabled(enabled);
        return userRepository.saveAndFlush(account);
    }
}
