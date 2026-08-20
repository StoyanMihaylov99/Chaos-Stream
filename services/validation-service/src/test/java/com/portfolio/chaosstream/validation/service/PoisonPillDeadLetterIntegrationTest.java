package com.portfolio.chaosstream.validation.service;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@EmbeddedKafka(partitions = 1, topics = {
        PoisonPillDeadLetterIntegrationTest.INGESTED_TOPIC,
        PoisonPillDeadLetterIntegrationTest.DLT_TOPIC
})
class PoisonPillDeadLetterIntegrationTest {

    static final String INGESTED_TOPIC = "transactions.ingested.v1";
    static final String DLT_TOPIC = "transactions.ingested.v1.DLT";

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private KafkaTemplate<String, String> rawKafkaTemplate;
    private Consumer<String, String> dltConsumer;

    @BeforeEach
    void setUp() {
        Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafkaBroker);
        rawKafkaTemplate = new KafkaTemplate<>(
                new DefaultKafkaProducerFactory<>(producerProps, new StringSerializer(), new StringSerializer()));

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("poison-pill-test-consumer", "true", embeddedKafkaBroker);
        dltConsumer = new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(), new StringDeserializer())
                .createConsumer();
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(dltConsumer, DLT_TOPIC);
    }

    @AfterEach
    void tearDown() {
        dltConsumer.close();
    }

    @Test
    void malformedMessage_getsDeadLettered() {
        rawKafkaTemplate.send(new ProducerRecord<>(INGESTED_TOPIC, "poison-pill-key", "not-valid-json"));

        ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(dltConsumer, DLT_TOPIC, Duration.ofSeconds(10));

        assertNotNull(record);
        assertEquals("poison-pill-key", record.key());
    }
}
