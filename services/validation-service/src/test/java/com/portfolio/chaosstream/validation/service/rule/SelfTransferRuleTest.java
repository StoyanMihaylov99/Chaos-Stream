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

class SelfTransferRuleTest {

    private final SelfTransferRule rule = new SelfTransferRule();

    @Test
    void validateUnsuccessful_transferToSameAccount_returnsViolation() {
        // Given
        TransactionEvent event = setupTransactionEvent(TransactionType.TRANSFER, "1234", "1234");

        // When
        Optional<String> violation = rule.validate(event);

        // Then
        assertTrue(violation.isPresent());
    }

    @Test
    void validateSuccessful_transferToDifferentAccount_returnsEmpty() {
        // Given; When
        TransactionEvent event = setupTransactionEvent(TransactionType.TRANSFER, "1234", "5678");

        // Then
        assertEquals(Optional.empty(), rule.validate(event));
    }

    @Test
    void validateSuccessful_nonTransferWithSameAccount_returnsEmpty() {
        // Given; When
        TransactionEvent event = setupTransactionEvent(TransactionType.DEPOSIT, "1234", "1234");

        // Then
        assertEquals(Optional.empty(), rule.validate(event));
    }

    private static TransactionEvent setupTransactionEvent(TransactionType type, String senderAccount, String receiverAccount) {
        return TransactionEvent.builder()
                .metaData(MetaData.builder()
                        .eventId("evt-1")
                        .timestamp(LocalDateTime.now())
                        .build())
                .payload(Payload.builder()
                        .transactionType(type)
                        .amount(BigDecimal.TEN)
                        .currency(Currency.USD)
                        .senderAccount(senderAccount)
                        .receiverAccount(receiverAccount)
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
