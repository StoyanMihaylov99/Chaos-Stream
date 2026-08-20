package com.portfolio.chaosstream.validation.service;

import com.portfolio.chaosstream.model.TransactionEvent;
import com.portfolio.chaosstream.validation.model.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class TransactionConsumerService {

    private static final Logger log = LoggerFactory.getLogger(TransactionConsumerService.class);

    private final TransactionValidationService transactionValidationService;
    private final TransactionOutcomeProducerService transactionOutcomeProducerService;

    public TransactionConsumerService(TransactionValidationService transactionValidationService,
                                       TransactionOutcomeProducerService transactionOutcomeProducerService) {
        this.transactionValidationService = transactionValidationService;
        this.transactionOutcomeProducerService = transactionOutcomeProducerService;
    }

    @KafkaListener(topics = "${app.kafka.topics.transactions-ingested}")
    public void consume(TransactionEvent event) {
        ValidationResult result = transactionValidationService.validate(event);
        if (result.isValid()) {
            log.info("Transaction event passed validation: {}", event);
            transactionOutcomeProducerService.publishValidated(event);
        } else {
            log.warn("Transaction event failed validation: violations={}, event={}", result.violations(), event);
            transactionOutcomeProducerService.publishRejected(event, result.violations());
        }
    }

}
