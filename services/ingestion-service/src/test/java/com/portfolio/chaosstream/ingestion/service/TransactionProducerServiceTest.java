package com.portfolio.chaosstream.ingestion.service;

import com.portfolio.chaosstream.exception.ApplicationException;
import com.portfolio.chaosstream.exception.ErrorCode;
import com.portfolio.chaosstream.model.TransactionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import static com.portfolio.chaosstream.model.TransactionEvent.*;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionProducerServiceTest {

    private static final String TOPIC_NAME = "transactions.ingested.v1";
    private static final long DEFAULT_TIMEOUT_MS = 3000;

    @Mock
    private KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    private TransactionProducerService transactionProducerService;

    @BeforeEach
    public void setUp() {
        this.transactionProducerService =
                new TransactionProducerService(kafkaTemplate, TOPIC_NAME, DEFAULT_TIMEOUT_MS);
    }

    @Test
    public void testPublish_successful() {
        // Given
        String key = "1234";
        TransactionEvent event = setupTransactionEvent(key);

        when(kafkaTemplate.send(TOPIC_NAME, key, event))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        // When
        assertDoesNotThrow(() -> transactionProducerService.publish(event));

        // Then
        verify(kafkaTemplate).send(TOPIC_NAME, key, event);
    }

    @Test
    public void testPublish_kafkaSendFails_throwsServiceUnavailable() {
        // Given
        String key = "1234";
        TransactionEvent event = setupTransactionEvent(key);

        when(kafkaTemplate.send(TOPIC_NAME, key, event))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker exploded")));

        // When
        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> transactionProducerService.publish(event));

        // Then
        assertEquals(ErrorCode.SERVICE_UNAVAILABLE, exception.getErrorCode());
    }

    @Test
    public void testPublish_timesOut_throwsServiceUnavailable() {
        // Given
        TransactionProducerService shortTimeoutProducer =
                new TransactionProducerService(kafkaTemplate, TOPIC_NAME, 50);
        String key = "1234";
        TransactionEvent event = setupTransactionEvent(key);

        when(kafkaTemplate.send(TOPIC_NAME, key, event))
                .thenReturn(new CompletableFuture<>());

        // When
        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> shortTimeoutProducer.publish(event));

        // Then
        assertEquals(ErrorCode.SERVICE_UNAVAILABLE, exception.getErrorCode());
    }

    private static TransactionEvent setupTransactionEvent(String key) {
        return builder()
                .metaData(MetaData.builder()
                        .eventId("evt-1")
                        .timestamp(LocalDateTime.now())
                        .build())
                .payload(Payload.builder()
                        .transactionType(TransactionType.TRANSFER)
                        .amount(BigDecimal.TEN)
                        .currency(Currency.USD)
                        .senderAccount(key)
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
