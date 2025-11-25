package ru.practicum.ewm.stats.aggregator.service;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.aggregator.kafka.KafkaConsumerConfig;
import ru.practicum.ewm.stats.aggregator.kafka.KafkaProducerConfig;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class UserActionEventProcessor implements Runnable {
    private final UserActionEventHandler userActionEventHandler;
    private final KafkaProducer<String, EventSimilarityAvro> kafkaProducer;
    private final KafkaConsumer<String, UserActionAvro> kafkaConsumer;
    private final String producerTopic;
    private final Duration poolTimeout;

    public UserActionEventProcessor(KafkaProducerConfig producerConfig,
                                    KafkaConsumerConfig consumerConfig,
                                    UserActionEventHandler userActionEventHandler) {

        this.userActionEventHandler = userActionEventHandler;
        kafkaProducer = new KafkaProducer<>(producerConfig.properties());
        producerTopic = producerConfig.topic();

        kafkaConsumer = new KafkaConsumer<>(consumerConfig.properties());
        kafkaConsumer.subscribe(List.of(consumerConfig.topic()));
        poolTimeout = consumerConfig.poolTimeout();
    }

    @PreDestroy
    public void shutDown() {
        log.info("Shutting down UserActionEventProcessor. Send wakeup to kafka consumer");
        kafkaConsumer.wakeup();
    }

    // kafka consumer pool loop
    @Override
    public void run() {
        try {
            while (true) {
                ConsumerRecords<String, UserActionAvro> records = kafkaConsumer.poll(poolTimeout);
                for (ConsumerRecord<String, UserActionAvro> record : records) {
                    Map<Long, Map<Long, Double>> similarities = userActionEventHandler.handle(record.value());

                    log.debug("Handle from topic: '{}' event: '{}'", record.topic(), record.value());

                    for (Long eventA : similarities.keySet()) {
                        for (Map.Entry<Long, Double> entry : similarities.get(eventA).entrySet()) {
                            EventSimilarityAvro message = EventSimilarityAvro.newBuilder()
                                    .setEventA(eventA)
                                    .setEventB(entry.getKey())
                                    .setScore(entry.getValue())
                                    .setTimestamp(record.value().getTimestamp())
                                    .build();

                            log.debug("Sending in topic: '{}' message: {}", producerTopic, message);
                            ProducerRecord<String, EventSimilarityAvro> producerRecord = new ProducerRecord<>(producerTopic, message);
                            kafkaProducer.send(producerRecord);
                        }
                    }
                }
            }
        } catch (WakeupException ignored) {
        } catch (Exception ex) {
            log.error("Error processing sensor events", ex);
        } finally {
            log.info("Closing Kafka Consumer");
            kafkaConsumer.close(Duration.ofSeconds(10));

            log.info("Closing Kafka Producer");
            kafkaProducer.close(Duration.ofSeconds(10));
        }
    }
}
