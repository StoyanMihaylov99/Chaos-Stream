package com.portfolio.chaosstream.gateway.error;

import com.portfolio.chaosstream.exception.ErrorCode;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Renders a GlobalErrorResponse body for authentication failures from the JWT resource
 * server (missing, malformed, or invalid bearer token) instead of Spring Security's default
 * empty 401 body.
 */
@Component
public class JsonAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

    private final GatewayErrorResponseWriter errorResponseWriter;

    public JsonAuthenticationEntryPoint(GatewayErrorResponseWriter errorResponseWriter) {
        this.errorResponseWriter = errorResponseWriter;
    }

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
        return errorResponseWriter.write(exchange, ErrorCode.UNAUTHORIZED);
    }
}
