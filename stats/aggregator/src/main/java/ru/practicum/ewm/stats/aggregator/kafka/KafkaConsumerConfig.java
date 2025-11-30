package ru.practicum.ewm.stats.aggregator.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Properties;

@ConfigurationProperties("aggregator.kafka.consumer")
public record KafkaConsumerConfig(Properties properties, String topic, Duration poolTimeout) {
}
