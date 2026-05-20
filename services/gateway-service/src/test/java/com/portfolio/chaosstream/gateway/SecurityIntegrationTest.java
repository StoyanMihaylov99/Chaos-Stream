package com.portfolio.chaosstream.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import reactor.core.publisher.Mono;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class SecurityIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ReactiveJwtDecoder jwtDecoder;

    @MockitoBean
    private RateLimiter rateLimiter;

    @Autowired
    private KeyResolver keyResolver;

    @Test
    void whenAuthenticated_thenResolveToUsername() {
        // Given
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "test-user")
                .build();
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, Collections.emptyList(), "test-user");

        ServerWebExchange exchange = mock(ServerWebExchange.class);
        when(exchange.getPrincipal()).thenReturn(Mono.just(authentication));

        // When
        String result = keyResolver.resolve(exchange).block();

        // Then
        assertEquals("test-user", result);
    }

    @Test
    void whenAnonymous_thenResolveToAnonymous() {
        // Given
        ServerWebExchange exchange = mock(ServerWebExchange.class);
        when(exchange.getPrincipal()).thenReturn(Mono.empty());

        // When
        String result = keyResolver.resolve(exchange).block();

        // Then
        assertEquals("anonymous", result);
    }

    @Test
    void whenUnauthenticated_thenUnauthorized() {
        // Given, When, Then
        webTestClient.get()
                .uri("/api/v1/transactions")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void whenAuthenticatedWithJwt_thenOk() {
        // Given
        Jwt jwt = Jwt.withTokenValue("mock-token")
                .header("alg", "none")
                .claim("sub", "test-user")
                .claim("scope", "message.read")
                .build();
        when(jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));
        when(rateLimiter.isAllowed(anyString(), anyString()))
                .thenReturn(Mono.just(new RateLimiter.Response(true, Collections.emptyMap())));

        // When, Then
        webTestClient.get()
                .uri("/api/v1/transactions")
                .header("Authorization", "Bearer mock-token")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void whenInvalidToken_thenUnauthorized() {
        // Given
        when(jwtDecoder.decode(anyString())).thenReturn(Mono.error(new BadCredentialsException("Invalid token")));

        // When, Then
        webTestClient.get()
                .uri("/api/v1/transactions")
                .header("Authorization", "Bearer invalid-token")
                .exchange()
                .expectStatus().isUnauthorized();
    }

}
