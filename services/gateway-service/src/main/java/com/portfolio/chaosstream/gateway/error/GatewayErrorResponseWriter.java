package com.portfolio.chaosstream.gateway.error;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.chaosstream.exception.ErrorCode;
import com.portfolio.chaosstream.exception.GlobalErrorResponse;
import com.portfolio.chaosstream.exception.TraceIdSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
public class GatewayErrorResponseWriter {

    private static final Logger log = LoggerFactory.getLogger(GatewayErrorResponseWriter.class);

    private final ObjectMapper objectMapper;

    public GatewayErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Mono<Void> write(ServerWebExchange exchange, ErrorCode errorCode) {
        return write(exchange, errorCode, errorCode.getDefaultMessage());
    }

    public Mono<Void> write(ServerWebExchange exchange, ErrorCode errorCode, String message) {
        String traceId = TraceIdSupport.resolve(exchange.getRequest().getHeaders().getFirst(TraceIdSupport.HEADER));
        return write(exchange.getResponse(), exchange.getRequest().getPath().value(), errorCode, message, traceId);
    }

    public Mono<Void> write(ServerHttpResponse response, String path, ErrorCode errorCode, String message,
            String traceId) {
        response.setStatusCode(errorCode.getStatus());
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().set(TraceIdSupport.HEADER, traceId);

        GlobalErrorResponse body = GlobalErrorResponse.builder()
                .timestamp(Instant.now())
                .status(errorCode.getStatus().value())
                .error(errorCode.name())
                .message(message)
                .path(path)
                .traceId(traceId)
                .build();

        byte[] bytes = serialize(body, errorCode);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }

    private byte[] serialize(GlobalErrorResponse body, ErrorCode errorCode) {
        try {
            return objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize error response for {}", errorCode, e);
            return ("{\"error\":\"" + errorCode.name() + "\"}").getBytes(StandardCharsets.UTF_8);
        }
    }
}
