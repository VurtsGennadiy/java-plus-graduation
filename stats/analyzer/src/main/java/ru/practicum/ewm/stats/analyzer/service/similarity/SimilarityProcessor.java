package ru.practicum.ewm.stats.analyzer.service.similarity;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.analyzer.config.SimilarityProcKafkaConfig;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.time.Duration;
import java.util.List;

/**
 * Запускает цикл опроса и обработки схожести мероприятий
 */
@Service
@Slf4j
public class SimilarityProcessor implements Runnable {
    private final SimilarityService similarityService;
    private final KafkaConsumer<String, EventSimilarityAvro> kafkaConsumer;
    private final Duration poolTimeout;

    public SimilarityProcessor(SimilarityProcKafkaConfig kafkaConfig, SimilarityService similarityService) {
        this.similarityService = similarityService;
        kafkaConsumer = new KafkaConsumer<>(kafkaConfig.properties());
        kafkaConsumer.subscribe(List.of(kafkaConfig.topic()));
        poolTimeout = kafkaConfig.poolTimeout();
    }

    @PreDestroy
    public void shutDown() {
        log.info("Shutting down Similarity Processor. Send wakeup to kafka consumer");
        kafkaConsumer.wakeup();
    }

    // kafka consumer pool loop
    @Override
    public void run() {
        log.info("Similarity Processor was started");
        try {
            while (true) {
                ConsumerRecords<String, EventSimilarityAvro> records = kafkaConsumer.poll(poolTimeout);
                for (ConsumerRecord<String, EventSimilarityAvro> record : records) {
                    log.debug("Handle from topic: '{}' message: '{}'", record.topic(), record.value());
                    similarityService.handleSimilarity(record.value());
                }
            }
        } catch (WakeupException ignored) {
        } catch (Exception ex) {
            log.error("Error processing sensor events", ex);
        } finally {
            log.info("Closing Kafka Consumer");
            kafkaConsumer.close(Duration.ofSeconds(10));
        }
    }
}
