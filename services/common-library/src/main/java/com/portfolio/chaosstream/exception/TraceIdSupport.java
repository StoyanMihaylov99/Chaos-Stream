package com.portfolio.chaosstream.exception;

import java.util.UUID;

public final class TraceIdSupport {

    public static final String HEADER = "X-Trace-Id";

    private TraceIdSupport() {
    }

    public static String resolve(String incoming) {
        return (incoming != null && !incoming.isBlank()) ? incoming : UUID.randomUUID().toString();
    }
}
