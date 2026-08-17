package com.portfolio.chaosstream.gateway.error;

import com.portfolio.chaosstream.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

/**
 * Catches exceptions that propagate up through the gateway's WebFlux/route filter chain:
 * no route matching the request (404), the downstream being unreachable (503) or timing
 * out (504), and anything else unexpected (500). Registered ahead of Spring Boot's
 * DefaultErrorWebExceptionHandler (@Order(-1)) so it gets first refusal.
 *
 * Out of scope here: 401/403 (handled by SecurityConfig's entry point/access denied handler,
 * which run before this in the Security filter chain) and 429 (the rate limiter completes
 * the response directly without raising an exception; see RateLimiterErrorGlobalFilter).
 */
@Component
@Order(-2)
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorWebExceptionHandler.class);

    private final GatewayErrorResponseWriter errorResponseWriter;

    public GlobalErrorWebExceptionHandler(GatewayErrorResponseWriter errorResponseWriter) {
        this.errorResponseWriter = errorResponseWriter;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        ErrorCode errorCode = resolveErrorCode(ex);
        if (errorCode == ErrorCode.INTERNAL_SERVER_ERROR) {
            log.error("Unhandled gateway exception: {}", ex.getMessage(), ex);
        } else {
            log.warn("Gateway exception [{}]: {}", errorCode, ex.getMessage());
        }

        return errorResponseWriter.write(exchange, errorCode);
    }

    private ErrorCode resolveErrorCode(Throwable ex) {
        if (ex instanceof ResponseStatusException rse) {
            return switch (HttpStatus.valueOf(rse.getStatusCode().value())) {
                case NOT_FOUND -> ErrorCode.ROUTE_NOT_FOUND;
                case UNAUTHORIZED -> ErrorCode.UNAUTHORIZED;
                case FORBIDDEN -> ErrorCode.FORBIDDEN;
                case TOO_MANY_REQUESTS -> ErrorCode.TOO_MANY_REQUESTS;
                case SERVICE_UNAVAILABLE -> ErrorCode.SERVICE_UNAVAILABLE;
                case GATEWAY_TIMEOUT -> ErrorCode.GATEWAY_TIMEOUT;
                default -> ErrorCode.INTERNAL_SERVER_ERROR;
            };
        }
        if (isTimeout(ex)) {
            return ErrorCode.GATEWAY_TIMEOUT;
        }
        if (isDownstreamUnreachable(ex)) {
            return ErrorCode.SERVICE_UNAVAILABLE;
        }
        return ErrorCode.INTERNAL_SERVER_ERROR;
    }

    private boolean isTimeout(Throwable ex) {
        return causeChainContains(ex, t -> t instanceof TimeoutException
                || t.getClass().getSimpleName().contains("TimeoutException"));
    }

    private boolean isDownstreamUnreachable(Throwable ex) {
        return causeChainContains(ex, t -> t instanceof ConnectException
                || t instanceof UnknownHostException);
    }

    private boolean causeChainContains(Throwable ex, java.util.function.Predicate<Throwable> predicate) {
        Throwable current = ex;
        for (int depth = 0; current != null && depth < 10; depth++, current = current.getCause()) {
            if (predicate.test(current)) {
                return true;
            }
        }
        return false;
    }
}
