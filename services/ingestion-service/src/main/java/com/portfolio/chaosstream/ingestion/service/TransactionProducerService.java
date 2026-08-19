package com.portfolio.chaosstream.ingestion.service;

import com.portfolio.chaosstream.exception.ApplicationException;
import com.portfolio.chaosstream.exception.ErrorCode;
import com.portfolio.chaosstream.model.TransactionEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class TransactionProducerService {

    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;
    private final String topicName;
    private final long publishTimeoutMillis;


    public TransactionProducerService(KafkaTemplate<String, TransactionEvent> kafkaTemplate,
                                      @Value("${app.kafka.topics.transactions-ingested}") String topicName,
                                      @Value("${app.kafka.producer.publish-timeout-ms:3000}") long publishTimeoutMillis) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
        this.publishTimeoutMillis = publishTimeoutMillis;
    }

    public void publish(TransactionEvent event) {
        String key = event.payload().senderAccount();

        try {
            kafkaTemplate.send(topicName, key, event)
                    .get(publishTimeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException timeoutException){
            throw new ApplicationException(ErrorCode.SERVICE_UNAVAILABLE, "Kafka broker did not acknowledge in time", timeoutException);
        } catch (InterruptedException interruptedException){
            Thread.currentThread().interrupt();
            throw new ApplicationException(ErrorCode.SERVICE_UNAVAILABLE, "Interrupted while publishing to Kafka", interruptedException);
        } catch (ExecutionException executionException){
            throw new ApplicationException(ErrorCode.SERVICE_UNAVAILABLE, "Failed to publish to Kafka", executionException.getCause());
        }
    }

}
