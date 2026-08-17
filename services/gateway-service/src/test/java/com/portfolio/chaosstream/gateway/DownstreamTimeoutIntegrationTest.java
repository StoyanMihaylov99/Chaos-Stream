package com.portfolio.chaosstream.gateway;

import org.junit.jupiter.api.AfterAll;
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
import java.net.Socket;
import java.time.Duration;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Points the transaction-route at a raw socket that accepts the connection but never writes
 * a response, so the gateway's configured response-timeout (see application.yaml) trips -
 * the real-world "downstream is up but too slow" case. response-timeout is overridden to a
 * short value here so the test doesn't have to wait out the production timeout.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.cloud.gateway.httpclient.response-timeout=300ms")
@AutoConfigureWebTestClient
class DownstreamTimeoutIntegrationTest {

    private static final ServerSocket HANGING_SERVER = startHangingServer();

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ReactiveJwtDecoder jwtDecoder;

    @MockBean
    private RateLimiter rateLimiter;

    private static ServerSocket startHangingServer() {
        try {
            ServerSocket socket = new ServerSocket(0);
            Thread acceptor = new Thread(() -> {
                while (!socket.isClosed()) {
                    try (Socket client = socket.accept()) {
                        Thread.sleep(Duration.ofSeconds(10).toMillis());
                    } catch (Exception ignored) {
                        // socket closed or client gave up; loop exits via the isClosed check
                    }
                }
            });
            acceptor.setDaemon(true);
            acceptor.start();
            return socket;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @AfterAll
    static void stopHangingServer() throws IOException {
        HANGING_SERVER.close();
    }

    @DynamicPropertySource
    static void routeToHangingServer(DynamicPropertyRegistry registry) {
        // The dynamic property source becomes authoritative for the whole routes[0] entry
        // once it defines any property there, so the full route must be redefined here
        // rather than just overriding uri - it won't merge with test/application.yml's copy.
        registry.add("spring.cloud.gateway.routes[0].id", () -> "transaction-route");
        registry.add("spring.cloud.gateway.routes[0].uri", () -> "http://localhost:" + HANGING_SERVER.getLocalPort());
        registry.add("spring.cloud.gateway.routes[0].predicates[0]", () -> "Path=/api/v1/transactions/**");
        registry.add("spring.cloud.gateway.routes[0].filters[0].name", () -> "RequestRateLimiter");
        registry.add("spring.cloud.gateway.routes[0].filters[0].args.key-resolver", () -> "#{@userKeyResolver}");
    }

    @Test
    void whenDownstreamTimesOut_thenGatewayTimeoutWithJsonErrorBody() {
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
                .expectStatus().isEqualTo(504)
                .expectBody()
                .jsonPath("$.status").isEqualTo(504)
                .jsonPath("$.error").isEqualTo("GATEWAY_TIMEOUT");
    }
}
