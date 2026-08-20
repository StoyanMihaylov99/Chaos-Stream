package com.portfolio.chaosstream.validation.service;

import com.portfolio.chaosstream.exception.ApplicationException;
import com.portfolio.chaosstream.exception.ErrorCode;
import com.portfolio.chaosstream.model.TransactionEvent;
import com.portfolio.chaosstream.validation.model.RejectedTransactionEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class TransactionOutcomeProducerService {

    private final KafkaTemplate<String, TransactionEvent> validatedKafkaTemplate;
    private final KafkaTemplate<String, RejectedTransactionEvent> rejectedKafkaTemplate;
    private final String validatedTopic;
    private final String rejectedTopic;
    private final long publishTimeoutMillis;

    public TransactionOutcomeProducerService(
            KafkaTemplate<String, TransactionEvent> validatedKafkaTemplate,
            KafkaTemplate<String, RejectedTransactionEvent> rejectedKafkaTemplate,
            @Value("${app.kafka.topics.transactions-validated}") String validatedTopic,
            @Value("${app.kafka.topics.transactions-dlq}") String rejectedTopic,
            @Value("${app.kafka.producer.publish-timeout-ms:3000}") long publishTimeoutMillis) {
        this.validatedKafkaTemplate = validatedKafkaTemplate;
        this.rejectedKafkaTemplate = rejectedKafkaTemplate;
        this.validatedTopic = validatedTopic;
        this.rejectedTopic = rejectedTopic;
        this.publishTimeoutMillis = publishTimeoutMillis;
    }

    public void publishValidated(TransactionEvent event) {
        String key = event.payload().senderAccount();
        await(validatedKafkaTemplate.send(validatedTopic, key, event));
    }

    public void publishRejected(TransactionEvent event, List<String> violations) {
        String key = event.payload().senderAccount();
        await(rejectedKafkaTemplate.send(rejectedTopic, key, new RejectedTransactionEvent(event, violations)));
    }

    private void  await(CompletableFuture<?> future) {
        try {
            future.get(publishTimeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException timeoutException) {
            throw new ApplicationException(ErrorCode.SERVICE_UNAVAILABLE, "Kafka broker did not acknowledge in time", timeoutException);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new ApplicationException(ErrorCode.SERVICE_UNAVAILABLE, "Interrupted while publishing to Kafka", interruptedException);
        } catch (ExecutionException executionException) {
            throw new ApplicationException(ErrorCode.SERVICE_UNAVAILABLE, "Failed to publish to Kafka", executionException.getCause());
        }
    }

}
