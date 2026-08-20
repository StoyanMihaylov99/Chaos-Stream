package com.portfolio.chaosstream.validation.service.rule;

import com.portfolio.chaosstream.model.TransactionEvent;
import com.portfolio.chaosstream.model.TransactionEvent.Currency;
import com.portfolio.chaosstream.model.TransactionEvent.MetaData;
import com.portfolio.chaosstream.model.TransactionEvent.Payload;
import com.portfolio.chaosstream.model.TransactionEvent.Security;
import com.portfolio.chaosstream.model.TransactionEvent.Status;
import com.portfolio.chaosstream.model.TransactionEvent.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FutureTimestampRuleTest {

    private final FutureTimestampRule rule = new FutureTimestampRule();

    @Test
    void validateSuccessful_timestampInPast_returnsEmpty() {
        // Given; // When
        TransactionEvent event = setupTransactionEvent(LocalDateTime.now().minusMinutes(5));

        // Then
        assertEquals(Optional.empty(), rule.validate(event));
    }

    @Test
    void validateUnsuccessful_timestampWithinClockSkewTolerance_returnsEmpty() {
        // Given; When
        TransactionEvent event = setupTransactionEvent(LocalDateTime.now().plusSeconds(10));

        // Then
        assertEquals(Optional.empty(), rule.validate(event));
    }

    @Test
    void validateUnsuccessful_timestampFarInFuture_returnsViolation() {
        // Given
        TransactionEvent event = setupTransactionEvent(LocalDateTime.now().plusHours(1));

        // When
        Optional<String> violation = rule.validate(event);

        // Then
        assertTrue(violation.isPresent());
    }

    private static TransactionEvent setupTransactionEvent(LocalDateTime timestamp) {
        return TransactionEvent.builder()
                .metaData(MetaData.builder()
                        .eventId("evt-1")
                        .timestamp(timestamp)
                        .build())
                .payload(Payload.builder()
                        .transactionType(TransactionType.TRANSFER)
                        .amount(BigDecimal.TEN)
                        .currency(Currency.USD)
                        .senderAccount("1234")
                        .receiverAccount("5678")
                        .status(Status.PENDING)
                        .idempotencyKey("idem-1")
                        .build())
                .security(Security.builder()
                        .userId("user_99")
                        .clientId("transaction-producer-01")
                        .build())
                .build();
    }
}
