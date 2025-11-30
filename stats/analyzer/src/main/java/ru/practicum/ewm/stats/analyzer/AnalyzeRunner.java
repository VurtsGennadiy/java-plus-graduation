package ru.practicum.ewm.stats.analyzer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.analyzer.service.interaction.InteractionProcessor;
import ru.practicum.ewm.stats.analyzer.service.similarity.SimilarityProcessor;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyzeRunner implements CommandLineRunner {
    private final InteractionProcessor interactionProcessor;
    private final SimilarityProcessor similarityProcessor;

    @Override
    public void run(String... args) {
        log.info("Starting Interaction Processor");
        Thread interactionProcessorThread = new Thread(interactionProcessor);
        interactionProcessorThread.setName("InteractionProc");
        interactionProcessorThread.start();

        log.info("Starting Similarity Processor");
        Thread similarityProcessorThread = new Thread(similarityProcessor);
        similarityProcessorThread.setName("SimilarityProc");
        similarityProcessorThread.start();
    }
}
