package ru.practicum.ewm.stats.analyzer.service.similarity;

import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

public interface SimilarityService {
    void handleSimilarity(EventSimilarityAvro avro);
}
