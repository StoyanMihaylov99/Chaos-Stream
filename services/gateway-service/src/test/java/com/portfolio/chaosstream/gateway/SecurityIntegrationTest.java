package com.portfolio.chaosstream.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.http.server.reactive.ServerHttpRequest;
import java.net.InetSocketAddress;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.BeforeEach;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class SecurityIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ReactiveJwtDecoder jwtDecoder;

    @MockBean
    private RateLimiter rateLimiter;

    @Autowired
    @Qualifier("userKeyResolver")
    private KeyResolver keyResolver;

    @BeforeEach
    void setUpRateLimiter() {
        when(rateLimiter.isAllowed(anyString(), anyString()))
                .thenReturn(Mono.just(new RateLimiter.Response(true, Collections.emptyMap())));
    }

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
    void whenAnonymous_thenResolveToIp() {
        // Given
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 80);
        when(request.getRemoteAddress()).thenReturn(address);

        ServerWebExchange exchange = mock(ServerWebExchange.class);
        when(exchange.getPrincipal()).thenReturn(Mono.empty());
        when(exchange.getRequest()).thenReturn(request);

        // When
        String result = keyResolver.resolve(exchange).block();

        // Then
        assertEquals("127.0.0.1", result);
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
