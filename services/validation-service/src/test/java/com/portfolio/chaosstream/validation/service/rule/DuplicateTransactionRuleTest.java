package com.portfolio.chaosstream.validation.service.rule;

import com.portfolio.chaosstream.model.TransactionEvent;
import com.portfolio.chaosstream.model.TransactionEvent.Currency;
import com.portfolio.chaosstream.model.TransactionEvent.MetaData;
import com.portfolio.chaosstream.model.TransactionEvent.Payload;
import com.portfolio.chaosstream.model.TransactionEvent.Security;
import com.portfolio.chaosstream.model.TransactionEvent.Status;
import com.portfolio.chaosstream.model.TransactionEvent.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DuplicateTransactionRuleTest {

    private DuplicateTransactionRule rule;

    @BeforeEach
    void setUp() {
        rule = new DuplicateTransactionRule();
    }

    @Test
    void validateSuccessful_firstOccurrenceOfIdempotencyKey_returnsEmpty() {
        // Given; When
        TransactionEvent event = setupTransactionEvent("idem-1");

        // Then
        assertEquals(Optional.empty(), rule.validate(event));
    }

    @Test
    void validateUnsuccessful_repeatedIdempotencyKey_returnsViolation() {
        // Given
        TransactionEvent first = setupTransactionEvent("idem-1");
        TransactionEvent duplicate = setupTransactionEvent("idem-1");

        // When
        rule.validate(first);
        Optional<String> violation = rule.validate(duplicate);

        // Then
        assertTrue(violation.isPresent());
    }

    @Test
    void validateSuccessful_differentIdempotencyKeys_returnsEmptyForBoth() {
        // Given; When
        TransactionEvent first = setupTransactionEvent("idem-1");
        TransactionEvent second = setupTransactionEvent("idem-2");

        // Then
        assertEquals(Optional.empty(), rule.validate(first));
        assertEquals(Optional.empty(), rule.validate(second));
    }

    private static TransactionEvent setupTransactionEvent(String idempotencyKey) {
        return TransactionEvent.builder()
                .metaData(MetaData.builder()
                        .eventId("evt-1")
                        .timestamp(LocalDateTime.now())
                        .build())
                .payload(Payload.builder()
                        .transactionType(TransactionType.TRANSFER)
                        .amount(BigDecimal.TEN)
                        .currency(Currency.USD)
                        .senderAccount("1234")
                        .receiverAccount("5678")
                        .status(Status.PENDING)
                        .idempotencyKey(idempotencyKey)
                        .build())
                .security(Security.builder()
                        .userId("user_99")
                        .clientId("transaction-producer-01")
                        .build())
                .build();
    }
}
