package ru.practicum.interaction.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.interaction.exception.GlobalExceptionHandler;

@Configuration
public class ExceptionHandlingConfig {
    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
