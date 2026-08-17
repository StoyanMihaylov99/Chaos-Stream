package com.portfolio.chaosstream.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GlobalErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, WebRequest request) {

        Map<String, List<String>> validationErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            validationErrors
                    .computeIfAbsent(fieldError.getField(), key -> new ArrayList<>())
                    .add(fieldError.getDefaultMessage());
        }

        log.warn("Validation failed: {}", validationErrors);

        return buildResponse(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.getDefaultMessage(),
                request, validationErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<GlobalErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, WebRequest request) {

        Map<String, List<String>> validationErrors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String field = lastPathSegment(violation.getPropertyPath());
            validationErrors
                    .computeIfAbsent(field, key -> new ArrayList<>())
                    .add(violation.getMessage());
        }

        log.warn("Validation failed: {}", validationErrors);

        return buildResponse(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.getDefaultMessage(),
                request, validationErrors);
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

        String traceId = TraceIdSupport.resolve(request.getHeader(TraceIdSupport.HEADER));

        GlobalErrorResponse error = GlobalErrorResponse.builder()
                .timestamp(Instant.now())
                .status(errorCode.getStatus().value())
                .error(errorCode.name())
                .message(message)
                .path(request.getDescription(false).replace("uri=", ""))
                .traceId(traceId)
                .validationErrors(validationErrors)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.add(TraceIdSupport.HEADER, traceId);

        return new ResponseEntity<>(error, headers, errorCode.getStatus());
    }

    private String lastPathSegment(Path path) {
        String field = null;
        for (Path.Node node : path) {
            field = node.getName();
        }
        return field != null ? field : path.toString();
    }
}
