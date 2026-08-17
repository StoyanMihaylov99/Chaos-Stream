package com.portfolio.chaosstream.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Points the transaction-route at a port with nothing listening, so the gateway's routing
 * filter gets a connection refused - the real-world "downstream unreachable" case.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class DownstreamUnreachableIntegrationTest {

    // Reserve a port and release it immediately, so nothing is listening but the OS is
    // unlikely to hand it to something else before this test runs.
    private static final int CLOSED_PORT = reserveAndReleasePort();

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ReactiveJwtDecoder jwtDecoder;

    @MockBean
    private RateLimiter rateLimiter;

    private static int reserveAndReleasePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @DynamicPropertySource
    static void routeToUnreachableHost(DynamicPropertyRegistry registry) {
        // The dynamic property source becomes authoritative for the whole routes[0] entry
        // once it defines any property there, so the full route must be redefined here
        // rather than just overriding uri - it won't merge with test/application.yml's copy.
        registry.add("spring.cloud.gateway.routes[0].id", () -> "transaction-route");
        registry.add("spring.cloud.gateway.routes[0].uri", () -> "http://localhost:" + CLOSED_PORT);
        registry.add("spring.cloud.gateway.routes[0].predicates[0]", () -> "Path=/api/v1/transactions/**");
        registry.add("spring.cloud.gateway.routes[0].filters[0].name", () -> "RequestRateLimiter");
        registry.add("spring.cloud.gateway.routes[0].filters[0].args.key-resolver", () -> "#{@userKeyResolver}");
    }

    @Test
    void whenDownstreamUnreachable_thenServiceUnavailableWithJsonErrorBody() {
        when(rateLimiter.isAllowed(anyString(), anyString()))
                .thenReturn(Mono.just(new RateLimiter.Response(true, Collections.emptyMap())));

        Jwt jwt = Jwt.withTokenValue("mock-token")
                .header("alg", "none")
                .claim("sub", "test-user")
                .claim("scope", "message.read")
                .build();
        when(jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));

        webTestClient.get()
                .uri("/api/v1/transactions")
                .header("Authorization", "Bearer mock-token")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.status").isEqualTo(503)
                .jsonPath("$.error").isEqualTo("SERVICE_UNAVAILABLE");
    }
}
