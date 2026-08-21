package com.portfolio.chaosstream.storage.service;

import com.portfolio.chaosstream.model.TransactionEvent;
import com.portfolio.chaosstream.model.TransactionEvent.Currency;
import com.portfolio.chaosstream.model.TransactionEvent.MetaData;
import com.portfolio.chaosstream.model.TransactionEvent.Payload;
import com.portfolio.chaosstream.model.TransactionEvent.Security;
import com.portfolio.chaosstream.model.TransactionEvent.Status;
import com.portfolio.chaosstream.model.TransactionEvent.TransactionType;
import com.portfolio.chaosstream.storage.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
})
@EmbeddedKafka(partitions = 1, topics = TransactionConsumerServiceIntegrationTest.VALIDATED_TOPIC)
class TransactionConsumerServiceIntegrationTest {

    static final String VALIDATED_TOPIC = "transactions.validated.v1";

    @Autowired
    private KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    @MockBean
    private TransactionRepository transactionRepository;

    @SpyBean
    private TransactionPersistenceService transactionPersistenceService;

    @Test
    void validatedEvent_triggersPersistence() {
        TransactionEvent event = setupTransactionEvent("idem-wiring-1");

        kafkaTemplate.send(VALIDATED_TOPIC, event.payload().senderAccount(), event);

        verify(transactionPersistenceService, timeout(5000)).persist(eq(event));
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
