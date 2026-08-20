package com.portfolio.chaosstream.validation.service;

import com.portfolio.chaosstream.model.TransactionEvent;
import com.portfolio.chaosstream.model.TransactionEvent.Currency;
import com.portfolio.chaosstream.model.TransactionEvent.MetaData;
import com.portfolio.chaosstream.model.TransactionEvent.Payload;
import com.portfolio.chaosstream.model.TransactionEvent.Security;
import com.portfolio.chaosstream.model.TransactionEvent.Status;
import com.portfolio.chaosstream.model.TransactionEvent.TransactionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@EmbeddedKafka(partitions = 1, topics = TransactionOutcomeRoutingIntegrationTest.INGESTED_TOPIC)
class TransactionOutcomeRoutingIntegrationTest {

    static final String INGESTED_TOPIC = "transactions.ingested.v1";

    @Autowired
    private KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    @SpyBean
    private TransactionOutcomeProducerService transactionOutcomeProducerService;

    @Test
    void validEvent_getsPublishedAsValidated() {
        TransactionEvent event = setupTransactionEvent(TransactionType.TRANSFER, "1234", "5678", "idem-route-valid");

        kafkaTemplate.send(INGESTED_TOPIC, event.payload().senderAccount(), event);

        verify(transactionOutcomeProducerService, timeout(5000)).publishValidated(eq(event));
    }

    @Test
    void selfTransferEvent_getsPublishedAsRejected() {
        TransactionEvent event = setupTransactionEvent(TransactionType.TRANSFER, "1234", "1234", "idem-route-selftransfer");

        kafkaTemplate.send(INGESTED_TOPIC, event.payload().senderAccount(), event);

        verify(transactionOutcomeProducerService, timeout(5000)).publishRejected(eq(event),
                eq(List.of("Sender and receiver account must not be the same for a transfer")));
    }

    private static TransactionEvent setupTransactionEvent(TransactionType type, String senderAccount,
                                                            String receiverAccount, String idempotencyKey) {
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
                        .idempotencyKey(idempotencyKey)
                        .build())
                .security(Security.builder()
                        .userId("user_99")
                        .clientId("transaction-producer-01")
                        .build())
                .build();
    }
}
