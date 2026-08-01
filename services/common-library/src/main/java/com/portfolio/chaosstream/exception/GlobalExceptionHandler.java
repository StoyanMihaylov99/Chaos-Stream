package com.portfolio.chaosstream.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<GlobalErrorResponse> handleApplicationException(
            ApplicationException ex, WebRequest request) {

        log.warn("Application exception [{}]: {}", ex.getErrorCode(), ex.getMessage());

        return buildResponse(ex.getErrorCode(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GlobalErrorResponse> handleAllExceptions(
            Exception ex, WebRequest request) {

        log.error("Unhandled exception: {}", ex.getMessage(), ex);

        return buildResponse(ErrorCode.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_SERVER_ERROR.getDefaultMessage(),
                request, null);
    }

    private ResponseEntity<GlobalErrorResponse> buildResponse(
            ErrorCode errorCode, String message, WebRequest request, Map<String, List<String>> validationErrors) {

        GlobalErrorResponse error = GlobalErrorResponse.builder()
                .timestamp(Instant.now())
                .status(errorCode.getStatus().value())
                .error(errorCode.name())
                .message(message)
                .path(request.getDescription(false).replace("uri=", ""))
                .validationErrors(validationErrors)
                .build();

        return new ResponseEntity<>(error, errorCode.getStatus());
    }
}
