package com.portfolio.chaosstream.validation.service;

import com.portfolio.chaosstream.model.TransactionEvent;
import com.portfolio.chaosstream.model.TransactionEvent.MetaData;
import com.portfolio.chaosstream.model.TransactionEvent.Payload;
import com.portfolio.chaosstream.model.TransactionEvent.Security;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@EmbeddedKafka(partitions = 1, topics = TransactionConsumerServiceIntegrationTest.TOPIC_NAME)
class TransactionConsumerServiceIntegrationTest {

    static final String TOPIC_NAME = "transactions.ingested.v1";

    @Autowired
    private KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    @SpyBean
    private TransactionConsumerService transactionConsumerService;

    @Test
    void consume_receivesPublishedTransactionEvent() {
        // Given
        TransactionEvent event = setupTransactionEvent("1234");

        // When
        kafkaTemplate.send(TOPIC_NAME, event.payload().senderAccount(), event);

        // Then
        verify(transactionConsumerService, timeout(5000)).consume(eq(event));
    }

    private static TransactionEvent setupTransactionEvent(String senderAccount) {
        return TransactionEvent.builder()
                .metaData(MetaData.builder()
                        .eventId("evt-1")
                        .timestamp(LocalDateTime.now())
                        .build())
                .payload(Payload.builder()
                        .transactionType(TransactionEvent.TransactionType.TRANSFER)
                        .amount(BigDecimal.TEN)
                        .currency(TransactionEvent.Currency.USD)
                        .senderAccount(senderAccount)
                        .receiverAccount("5678")
                        .status(TransactionEvent.Status.PENDING)
                        .idempotencyKey("idem-1")
                        .build())
                .security(Security.builder()
                        .userId("user_99")
                        .clientId("transaction-producer-01")
                        .build())
                .build();
    }
}
