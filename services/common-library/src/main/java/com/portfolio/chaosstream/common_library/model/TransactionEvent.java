package com.portfolio.chaosstream.common_library.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Builder
public record TransactionEvent(@Valid @NotNull @JsonProperty("metadata") MetaData metaData,
                               @Valid @NotNull @JsonProperty("payload") Payload payload,
                               @Valid @NotNull @JsonProperty("security") Security security) {

    @Builder
    public record MetaData(@NotBlank @JsonProperty("event_id") String eventId,
                           @JsonProperty("trace_id") String traceId,
                           @NotNull @JsonProperty("timestamp") LocalDateTime timestamp,
                           @JsonProperty("producer_version") String producerVersion,
                           @JsonProperty("schema_version") String schemaVersion) {
    }

    @Builder
    public record Payload(@NotNull @JsonProperty("transaction_type") TransactionType transactionType,
                          @NotNull @Positive @JsonProperty("amount") BigDecimal amount,
                          @NotNull @JsonProperty("currency") Currency currency,
                          @NotBlank @JsonProperty("sender_account") String senderAccount,
                          @NotBlank @JsonProperty("receiver_account") String receiverAccount,
                          @NotNull @JsonProperty("status") Status status,
                          @NotBlank @JsonProperty("idempotency_key") String idempotencyKey) {
    }

    @Builder
    public record Security(@NotBlank @JsonProperty("user_id") String userId,
                           @NotBlank @JsonProperty("client_id") String clientId) {
    }

    public enum Currency {
        USD, EUR, JPY, AUD
    }

    public enum Status {
        PENDING, FAILED, SUCCESSFUL
    }

    public enum TransactionType {
        TRANSFER, DEPOSIT, WITHDRAW
    }
}
