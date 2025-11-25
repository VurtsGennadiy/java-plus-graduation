package ru.practicum.ewm.stats.aggregator.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Properties;

@ConfigurationProperties("aggregator.kafka.producer")
public record KafkaProducerConfig(Properties properties, String topic) {
}
