package com.portfolio.chaosstream.gateway.error;

import com.portfolio.chaosstream.exception.ErrorCode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Renders a GlobalErrorResponse body when an authenticated caller lacks the required scope
 * (e.g. hasAuthority("SCOPE_message.write")) instead of Spring Security's default empty
 * 403 body.
 */
@Component
public class JsonAccessDeniedHandler implements ServerAccessDeniedHandler {

    private final GatewayErrorResponseWriter errorResponseWriter;

    public JsonAccessDeniedHandler(GatewayErrorResponseWriter errorResponseWriter) {
        this.errorResponseWriter = errorResponseWriter;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException ex) {
        return errorResponseWriter.write(exchange, ErrorCode.FORBIDDEN);
    }
}
