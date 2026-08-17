package com.portfolio.chaosstream.gateway.filter;

import com.portfolio.chaosstream.exception.TraceIdSupport;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Ensures every request carries an X-Trace-Id header - generating one if the caller didn't
 * supply it - before it reaches Spring Security, routing, or the downstream service, so the
 * same id shows up in the gateway's error response and in whatever the backend logs/returns.
 * Ordered ahead of every other WebFilter (including Spring Security's chain) so they, and the
 * proxied request forwarded downstream, all see the resolved header.
 */
@Component
public class TraceIdWebFilter implements WebFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String traceId = TraceIdSupport.resolve(request.getHeaders().getFirst(TraceIdSupport.HEADER));

        ServerHttpRequest mutatedRequest = request.mutate()
                .header(TraceIdSupport.HEADER, traceId)
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
