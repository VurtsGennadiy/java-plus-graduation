package ru.practicum.ewm.stats.analyzer.service.interaction;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.analyzer.config.InteractionProcKafkaConfig;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Duration;
import java.util.List;

/**
 * Запускает цикл опроса и обработки событий действий пользователей
 */
@Service
@Slf4j
public class InteractionProcessor implements Runnable {
    private final InteractionService interactionService;
    private final KafkaConsumer<String, UserActionAvro> kafkaConsumer;
    private final Duration poolTimeout;

    public InteractionProcessor(InteractionProcKafkaConfig kafkaConfig, InteractionService interactionService) {
        this.interactionService = interactionService;
        kafkaConsumer = new KafkaConsumer<>(kafkaConfig.properties());
        kafkaConsumer.subscribe(List.of(kafkaConfig.topic()));
        poolTimeout = kafkaConfig.poolTimeout();
    }

    @PreDestroy
    public void shutDown() {
        log.info("Shutting down Interaction Processor. Send wakeup to kafka consumer");
        kafkaConsumer.wakeup();
    }

    // kafka consumer pool loop
    @Override
    public void run() {
        log.info("Interaction Processor was started");
        try {
            while (true) {
                ConsumerRecords<String, UserActionAvro> records = kafkaConsumer.poll(poolTimeout);
                for (ConsumerRecord<String, UserActionAvro> record : records) {
                    log.debug("Handle from topic: '{}' message: '{}'", record.topic(), record.value());
                    interactionService.handleInteraction(record.value());
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
