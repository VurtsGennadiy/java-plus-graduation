package ru.practicum.ewm.stats.aggregator;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.aggregator.service.UserActionEventProcessor;

@Component
@RequiredArgsConstructor
@Slf4j
public class AggregationRunner implements CommandLineRunner {
    private final UserActionEventProcessor userActionEventProcessor;
    @Override
    public void run(String... args) {
        log.info("Starting UserActionEventProcessor");

        Thread userActionEventProcessorThread = new Thread(userActionEventProcessor);
        userActionEventProcessorThread.setName("UserActionEventProc");
        userActionEventProcessorThread.start();
    }
}
