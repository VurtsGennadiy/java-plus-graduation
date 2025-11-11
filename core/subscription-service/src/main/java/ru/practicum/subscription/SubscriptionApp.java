package ru.practicum.subscription;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import ru.practicum.interaction.config.ExceptionHandlingConfig;
import ru.practicum.interaction.config.JacksonConfig;
import ru.practicum.interaction.config.LoggingConfig;

@SpringBootApplication
@EnableFeignClients("ru.practicum.interaction")
@Import({LoggingConfig.class, JacksonConfig.class, ExceptionHandlingConfig.class})
public class SubscriptionApp {
    public static void main(String[] args) {
        SpringApplication.run(SubscriptionApp.class, args);
    }
}
