package com.portfolio.chaosstream.storage.service;

import com.portfolio.chaosstream.model.TransactionEvent;
import com.portfolio.chaosstream.storage.entity.TransactionEntity;
import com.portfolio.chaosstream.storage.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class TransactionPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(TransactionPersistenceService.class);

    private final TransactionRepository transactionRepository;

    public TransactionPersistenceService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public void persist(TransactionEvent event) {
        TransactionEntity entity = toEntity(event);
        try {
            transactionRepository.save(entity);
            log.info("Persisted transaction: idempotencyKey={}", entity.getIdempotencyKey());
        } catch (DataIntegrityViolationException duplicateKey) {
            log.warn("Transaction already persisted, skipping: idempotencyKey={}", entity.getIdempotencyKey());
        }
    }

    private static TransactionEntity toEntity(TransactionEvent event) {
        return TransactionEntity.builder()
                .eventId(event.metaData().eventId())
                .traceId(event.metaData().traceId())
                .timestamp(event.metaData().timestamp())
                .producerVersion(event.metaData().producerVersion())
                .schemaVersion(event.metaData().schemaVersion())
                .transactionType(event.payload().transactionType())
                .amount(event.payload().amount())
                .currency(event.payload().currency())
                .senderAccount(event.payload().senderAccount())
                .receiverAccount(event.payload().receiverAccount())
                .status(event.payload().status())
                .idempotencyKey(event.payload().idempotencyKey())
                .userId(event.security().userId())
                .clientId(event.security().clientId())
                .build();
    }

}
