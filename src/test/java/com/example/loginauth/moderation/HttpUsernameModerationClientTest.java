package com.example.loginauth.moderation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.loginauth.common.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;

class HttpUsernameModerationClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;
    private String modelContent;
    private long responseDelayMillis;
    private volatile String requestBody;
    private HttpUsernameModerationClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/chat/completions", exchange -> {
            requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            if (responseDelayMillis > 0) {
                try {
                    Thread.sleep(responseDelayMillis);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            byte[] body = objectMapper.writeValueAsBytes(Map.of(
                    "choices", List.of(Map.of("message", Map.of("content", modelContent)))));
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        client = new HttpUsernameModerationClient(
                objectMapper,
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "test-api-key",
                "test-model",
                Duration.ofSeconds(2));
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void acceptsRequiredFieldsAlongsideAdditionalFields() throws Exception {
        modelContent = """
                {"decision":"ALLOW","reasonCode":"OK","reasonSummary":"Allowed","extra":"ignored"}
                """;

        UsernameReviewResult result = client.review("alice");

        assertThat(result.decision()).isEqualTo(ModerationDecision.ALLOW);
        assertThat(result.reasonCode()).isEqualTo("OK");
        assertThat(objectMapper.readTree(requestBody).path("enable_thinking").booleanValue()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"allow", "ALLOW"})
    void acceptsDecisionCaseDifferences(String decision) {
        modelContent = """
                {"decision":"%s","reasonCode":"OK","reasonSummary":"Allowed"}
                """.formatted(decision);

        assertThat(client.review("alice").decision()).isEqualTo(ModerationDecision.ALLOW);
    }

    @Test
    void unsupportedDecisionReturns503() {
        modelContent = """
                {"decision":"approved","reasonCode":"OK","reasonSummary":"Allowed"}
                """;

        assertUnavailable(() -> client.review("alice"));
    }

    @Test
    void malformedResponseReturns503() {
        modelContent = """
                {"decision":"ALLOW","reasonCode":"OK"}
                """;

        assertUnavailable(() -> client.review("alice"));
    }

    @Test
    void timeoutReturns503() {
        modelContent = """
                {"decision":"ALLOW","reasonCode":"OK","reasonSummary":"Allowed"}
                """;
        responseDelayMillis = 300;
        client = new HttpUsernameModerationClient(
                objectMapper,
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "test-api-key",
                "test-model",
                Duration.ofMillis(50));

        assertUnavailable(() -> client.review("alice"));
    }

    private void assertUnavailable(Runnable call) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(exception.getCode()).isEqualTo("MODERATION_UNAVAILABLE");
                });
    }
}
