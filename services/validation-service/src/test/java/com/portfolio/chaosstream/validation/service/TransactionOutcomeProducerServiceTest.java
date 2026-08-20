package com.portfolio.chaosstream.validation.service;

import com.portfolio.chaosstream.exception.ApplicationException;
import com.portfolio.chaosstream.exception.ErrorCode;
import com.portfolio.chaosstream.model.TransactionEvent;
import com.portfolio.chaosstream.model.TransactionEvent.Currency;
import com.portfolio.chaosstream.model.TransactionEvent.MetaData;
import com.portfolio.chaosstream.model.TransactionEvent.Payload;
import com.portfolio.chaosstream.model.TransactionEvent.Security;
import com.portfolio.chaosstream.model.TransactionEvent.Status;
import com.portfolio.chaosstream.model.TransactionEvent.TransactionType;
import com.portfolio.chaosstream.validation.model.RejectedTransactionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionOutcomeProducerServiceTest {

    private static final String VALIDATED_TOPIC = "transactions.validated.v1";
    private static final String DLQ_TOPIC = "transactions.dlq.v1";
    private static final long DEFAULT_TIMEOUT_MS = 3000;

    @Mock
    private KafkaTemplate<String, TransactionEvent> validatedKafkaTemplate;

    @Mock
    private KafkaTemplate<String, RejectedTransactionEvent> rejectedKafkaTemplate;

    private TransactionOutcomeProducerService transactionOutcomeProducerService;

    @BeforeEach
    void setUp() {
        transactionOutcomeProducerService = new TransactionOutcomeProducerService(
                validatedKafkaTemplate, rejectedKafkaTemplate, VALIDATED_TOPIC, DLQ_TOPIC, DEFAULT_TIMEOUT_MS);
    }

    @Test
    void publishValidated_successful_sendsToValidatedTopic() {
        String key = "1234";
        TransactionEvent event = setupTransactionEvent(key);

        when(validatedKafkaTemplate.send(VALIDATED_TOPIC, key, event))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        assertDoesNotThrow(() -> transactionOutcomeProducerService.publishValidated(event));

        verify(validatedKafkaTemplate).send(VALIDATED_TOPIC, key, event);
    }

    @Test
    void publishRejected_successful_sendsToDlqTopicWithViolations() {
        String key = "1234";
        TransactionEvent event = setupTransactionEvent(key);
        List<String> violations = List.of("Sender and receiver account must not be the same for a transfer");

        when(rejectedKafkaTemplate.send(eq(DLQ_TOPIC), eq(key), eq(new RejectedTransactionEvent(event, violations))))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        assertDoesNotThrow(() -> transactionOutcomeProducerService.publishRejected(event, violations));

        verify(rejectedKafkaTemplate).send(DLQ_TOPIC, key, new RejectedTransactionEvent(event, violations));
    }

    @Test
    void publishValidated_kafkaSendFails_throwsServiceUnavailable() {
        String key = "1234";
        TransactionEvent event = setupTransactionEvent(key);

        when(validatedKafkaTemplate.send(VALIDATED_TOPIC, key, event))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker exploded")));

        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> transactionOutcomeProducerService.publishValidated(event));

        assertEquals(ErrorCode.SERVICE_UNAVAILABLE, exception.getErrorCode());
    }

    @Test
    void publishRejected_timesOut_throwsServiceUnavailable() {
        TransactionOutcomeProducerService shortTimeoutProducer = new TransactionOutcomeProducerService(
                validatedKafkaTemplate, rejectedKafkaTemplate, VALIDATED_TOPIC, DLQ_TOPIC, 50);
        String key = "1234";
        TransactionEvent event = setupTransactionEvent(key);
        List<String> violations = List.of("some violation");

        when(rejectedKafkaTemplate.send(eq(DLQ_TOPIC), eq(key), eq(new RejectedTransactionEvent(event, violations))))
                .thenReturn(new CompletableFuture<>());

        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> shortTimeoutProducer.publishRejected(event, violations));

        assertEquals(ErrorCode.SERVICE_UNAVAILABLE, exception.getErrorCode());
    }

    private static TransactionEvent setupTransactionEvent(String senderAccount) {
        return TransactionEvent.builder()
                .metaData(MetaData.builder()
                        .eventId("evt-1")
                        .timestamp(LocalDateTime.now())
                        .build())
                .payload(Payload.builder()
                        .transactionType(TransactionType.TRANSFER)
                        .amount(BigDecimal.TEN)
                        .currency(Currency.USD)
                        .senderAccount(senderAccount)
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
