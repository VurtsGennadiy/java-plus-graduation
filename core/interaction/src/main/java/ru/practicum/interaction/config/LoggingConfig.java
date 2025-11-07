package ru.practicum.interaction.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import ru.practicum.interaction.logging.LoggingAspect;

@Configuration
@EnableAspectJAutoProxy
public class LoggingConfig {
    @Bean
    public LoggingAspect loggingAspect() {
        return new LoggingAspect();
    }
}
