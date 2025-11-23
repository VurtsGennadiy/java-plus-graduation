package ru.practicum.ewm.stats.collector.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@Slf4j
public class Producer implements AutoCloseable {
    private final String topic;
    private final KafkaProducer<String, SpecificRecordBase> kafkaProducer;

    public Producer(KafkaProducerConfig producerConfig) {
        kafkaProducer = new KafkaProducer<>(producerConfig.properties());
        topic = producerConfig.topics().getProperty("user-actions");
    }

    public void sendMessage(SpecificRecordBase message) {
        log.debug("Sending in topic: '{}' message: {}", topic, message);
        ProducerRecord<String, SpecificRecordBase> record = new ProducerRecord<>(topic, message);
        kafkaProducer.send(record, (metadata, exception) -> {
            if (exception != null) {
                log.error("Error sending message: {}", exception.getMessage());
            } else {
                log.trace("Message sent successfully. Topic {}, partition {}, offset {}",
                        metadata.topic(), metadata.partition(), metadata.offset());
            }
        });
    }

    @Override
    public void close() {
        kafkaProducer.close(Duration.ofSeconds(10));
        log.info("Kafka Producer closed");
    }
}
