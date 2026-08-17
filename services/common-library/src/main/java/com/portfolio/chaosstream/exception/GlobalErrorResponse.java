package com.portfolio.chaosstream.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GlobalErrorResponse(@NotNull Instant timestamp,
                                  @NotNull int status,
                                  @NotNull String error,
                                  @NotNull String message,
                                  @NotNull String path,
                                  @NotNull String traceId,
                                  @NotNull Map<String,List<String>> validationErrors) {
}
