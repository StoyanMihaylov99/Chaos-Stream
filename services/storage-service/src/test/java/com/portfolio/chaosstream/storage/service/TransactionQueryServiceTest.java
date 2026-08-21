package com.portfolio.chaosstream.storage.service;

import com.portfolio.chaosstream.exception.ApplicationException;
import com.portfolio.chaosstream.exception.ErrorCode;
import com.portfolio.chaosstream.model.TransactionEvent.Currency;
import com.portfolio.chaosstream.model.TransactionEvent.Status;
import com.portfolio.chaosstream.model.TransactionEvent.TransactionType;
import com.portfolio.chaosstream.storage.dto.TransactionResponse;
import com.portfolio.chaosstream.storage.entity.TransactionEntity;
import com.portfolio.chaosstream.storage.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionQueryServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    private TransactionQueryService transactionQueryService;

    @BeforeEach
    void setUp() {
        transactionQueryService = new TransactionQueryService(transactionRepository);
    }

    @Test
    void findByIdempotencyKey_found_returnsMappedResponse() {
        TransactionEntity entity = setupTransactionEntity("idem-1");
        when(transactionRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.of(entity));

        TransactionResponse response = transactionQueryService.findByIdempotencyKey("idem-1");

        assertEquals("idem-1", response.idempotencyKey());
        assertEquals("1234", response.senderAccount());
        assertEquals("5678", response.receiverAccount());
        assertEquals(BigDecimal.TEN, response.amount());
    }

    @Test
    void findByIdempotencyKey_notFound_throwsResourceNotFound() {
        when(transactionRepository.findByIdempotencyKey("missing")).thenReturn(Optional.empty());

        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> transactionQueryService.findByIdempotencyKey("missing"));

        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void findAll_mapsEntitiesToResponses() {
        TransactionEntity entity = setupTransactionEntity("idem-1");
        Pageable pageable = PageRequest.of(0, 10);
        Page<TransactionEntity> entityPage = new PageImpl<>(List.of(entity), pageable, 1);
        when(transactionRepository.findAll(pageable)).thenReturn(entityPage);

        Page<TransactionResponse> result = transactionQueryService.findAll(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("idem-1", result.getContent().get(0).idempotencyKey());
    }

    private static TransactionEntity setupTransactionEntity(String idempotencyKey) {
        return TransactionEntity.builder()
                .id(1L)
                .eventId("evt-1")
                .timestamp(LocalDateTime.now())
                .transactionType(TransactionType.TRANSFER)
                .amount(BigDecimal.TEN)
                .currency(Currency.USD)
                .senderAccount("1234")
                .receiverAccount("5678")
                .status(Status.PENDING)
                .idempotencyKey(idempotencyKey)
                .userId("user_99")
                .clientId("transaction-producer-01")
                .build();
    }
}
