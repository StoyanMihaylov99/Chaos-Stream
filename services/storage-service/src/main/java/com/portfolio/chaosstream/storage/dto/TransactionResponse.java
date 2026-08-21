package com.portfolio.chaosstream.storage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.portfolio.chaosstream.model.TransactionEvent.Currency;
import com.portfolio.chaosstream.model.TransactionEvent.Status;
import com.portfolio.chaosstream.model.TransactionEvent.TransactionType;
import com.portfolio.chaosstream.storage.entity.TransactionEntity;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record TransactionResponse(@JsonProperty("event_id") String eventId,
                                   @JsonProperty("trace_id") String traceId,
                                   @JsonProperty("timestamp") LocalDateTime timestamp,
                                   @JsonProperty("transaction_type") TransactionType transactionType,
                                   @JsonProperty("amount") BigDecimal amount,
                                   @JsonProperty("currency") Currency currency,
                                   @JsonProperty("sender_account") String senderAccount,
                                   @JsonProperty("receiver_account") String receiverAccount,
                                   @JsonProperty("status") Status status,
                                   @JsonProperty("idempotency_key") String idempotencyKey,
                                   @JsonProperty("user_id") String userId,
                                   @JsonProperty("client_id") String clientId) {

    public static TransactionResponse from(TransactionEntity entity) {
        return TransactionResponse.builder()
                .eventId(entity.getEventId())
                .traceId(entity.getTraceId())
                .timestamp(entity.getTimestamp())
                .transactionType(entity.getTransactionType())
                .amount(entity.getAmount())
                .currency(entity.getCurrency())
                .senderAccount(entity.getSenderAccount())
                .receiverAccount(entity.getReceiverAccount())
                .status(entity.getStatus())
                .idempotencyKey(entity.getIdempotencyKey())
                .userId(entity.getUserId())
                .clientId(entity.getClientId())
                .build();
    }
}
