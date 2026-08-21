package com.portfolio.chaosstream.storage.service;

import com.portfolio.chaosstream.model.TransactionEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class TransactionConsumerService {

    private final TransactionPersistenceService transactionPersistenceService;

    public TransactionConsumerService(TransactionPersistenceService transactionPersistenceService) {
        this.transactionPersistenceService = transactionPersistenceService;
    }

    @KafkaListener(topics = "${app.kafka.topics.transactions-validated}")
    public void consume(TransactionEvent event) {
        transactionPersistenceService.persist(event);
    }

}
