package com.portfolio.chaosstream.gateway.error;

import com.portfolio.chaosstream.exception.ErrorCode;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * RequestRateLimiterGatewayFilterFactory denies a request by setting the response status
 * and calling setComplete() directly, with no body and without raising an exception -
 * GlobalErrorWebExceptionHandler never sees it. This filter decorates the response early
 * in the chain so that when a downstream filter completes it as 429, a JSON error body is
 * written first.
 */
@Component
public class RateLimiterErrorGlobalFilter implements GlobalFilter, Ordered {

    private final GatewayErrorResponseWriter errorResponseWriter;

    public RateLimiterErrorGlobalFilter(GatewayErrorResponseWriter errorResponseWriter) {
        this.errorResponseWriter = errorResponseWriter;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpResponse originalResponse = exchange.getResponse();
        String path = exchange.getRequest().getPath().value();

        ServerHttpResponse decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> setComplete() {
                if (getStatusCode() == HttpStatus.TOO_MANY_REQUESTS && !isCommitted()) {
                    return errorResponseWriter.write(originalResponse, path,
                            ErrorCode.TOO_MANY_REQUESTS, ErrorCode.TOO_MANY_REQUESTS.getDefaultMessage());
                }
                return super.setComplete();
            }
        };

        return chain.filter(exchange.mutate().response(decoratedResponse).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
