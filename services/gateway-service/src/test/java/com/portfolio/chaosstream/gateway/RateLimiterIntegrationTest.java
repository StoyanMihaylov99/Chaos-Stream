package com.portfolio.chaosstream.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class RateLimiterIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ReactiveJwtDecoder jwtDecoder;

    @MockitoBean
    private RateLimiter rateLimiter;

    @Test
    void whenRateLimitExceeded_thenTooManyRequests() {
        // Given
        Jwt jwt = Jwt.withTokenValue("mock-token")
                .header("alg", "none")
                .claim("sub", "test-user")
                .claim("scope", "message.read")
                .build();
        when(jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));

        // Mock RateLimiter to deny request (Rate Limit Exceeded)
        when(rateLimiter.isAllowed(anyString(), anyString()))
                .thenReturn(Mono.just(new RateLimiter.Response(false, Map.of("X-Remaining-Tokens", "0"))));

        // When, Then
        webTestClient.get()
                .uri("/api/v1/transactions/limited")
                .header("Authorization", "Bearer mock-token")
                .exchange()
                .expectStatus().isEqualTo(429);
    }
}
