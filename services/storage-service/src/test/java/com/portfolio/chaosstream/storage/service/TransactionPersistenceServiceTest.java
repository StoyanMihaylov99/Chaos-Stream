package com.portfolio.chaosstream.storage.service;

import com.portfolio.chaosstream.model.TransactionEvent;
import com.portfolio.chaosstream.model.TransactionEvent.Currency;
import com.portfolio.chaosstream.model.TransactionEvent.MetaData;
import com.portfolio.chaosstream.model.TransactionEvent.Payload;
import com.portfolio.chaosstream.model.TransactionEvent.Security;
import com.portfolio.chaosstream.model.TransactionEvent.Status;
import com.portfolio.chaosstream.model.TransactionEvent.TransactionType;
import com.portfolio.chaosstream.storage.entity.TransactionEntity;
import com.portfolio.chaosstream.storage.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionPersistenceServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    private TransactionPersistenceService transactionPersistenceService;

    @BeforeEach
    void setUp() {
        transactionPersistenceService = new TransactionPersistenceService(transactionRepository);
    }

    @Test
    void persist_savesEntityMappedFromEvent() {
        TransactionEvent event = setupTransactionEvent("idem-1");

        transactionPersistenceService.persist(event);

        ArgumentCaptor<TransactionEntity> captor = ArgumentCaptor.forClass(TransactionEntity.class);
        verify(transactionRepository).save(captor.capture());
        TransactionEntity saved = captor.getValue();
        assertEquals("idem-1", saved.getIdempotencyKey());
        assertEquals("1234", saved.getSenderAccount());
        assertEquals("5678", saved.getReceiverAccount());
        assertEquals(BigDecimal.TEN, saved.getAmount());
        assertEquals(Currency.USD, saved.getCurrency());
        assertEquals(TransactionType.TRANSFER, saved.getTransactionType());
        assertEquals(Status.PENDING, saved.getStatus());
        assertEquals("user_99", saved.getUserId());
        assertEquals("transaction-producer-01", saved.getClientId());
    }

    @Test
    void persist_duplicateIdempotencyKey_doesNotThrow() {
        TransactionEvent event = setupTransactionEvent("idem-1");
        when(transactionRepository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertDoesNotThrow(() -> transactionPersistenceService.persist(event));
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
