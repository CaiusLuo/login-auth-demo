package com.example.loginauth.moderation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.loginauth.common.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class HttpUsernameModerationClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;
    private String modelContent;
    private HttpUsernameModerationClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/chat/completions", exchange -> {
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
    void acceptsRequiredFieldsAlongsideAdditionalFields() {
        modelContent = """
                {"decision":"ALLOW","reasonCode":"OK","reasonSummary":"Allowed","extra":"ignored"}
                """;

        UsernameReviewResult result = client.review("alice");

        assertThat(result.decision()).isEqualTo(ModerationDecision.ALLOW);
        assertThat(result.reasonCode()).isEqualTo("OK");
    }

    @Test
    void malformedResponseReturns503() {
        modelContent = """
                {"decision":"ALLOW","reasonCode":"OK"}
                """;

        assertThatThrownBy(() -> client.review("alice"))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(exception.getCode()).isEqualTo("MODERATION_UNAVAILABLE");
                });
    }
}
