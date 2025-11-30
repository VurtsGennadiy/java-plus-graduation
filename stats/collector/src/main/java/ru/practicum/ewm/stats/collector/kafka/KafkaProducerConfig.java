package ru.practicum.ewm.stats.collector.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Properties;

@ConfigurationProperties("collector.kafka.producer")
public record KafkaProducerConfig(Properties properties, Properties topics) {
}
