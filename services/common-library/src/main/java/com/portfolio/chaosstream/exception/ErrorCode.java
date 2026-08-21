package com.portfolio.chaosstream.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred"),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Request validation failed"),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "The service is temporarily unavailable"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Authentication is required or the provided credentials are invalid"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "You do not have permission to perform this action"),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded"),
    ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "No route matches the requested path"),
    GATEWAY_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "The downstream service did not respond in time"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "The requested resource was not found");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }
}
