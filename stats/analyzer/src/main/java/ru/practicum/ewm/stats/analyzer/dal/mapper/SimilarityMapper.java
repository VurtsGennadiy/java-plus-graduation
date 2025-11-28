package ru.practicum.ewm.stats.analyzer.dal.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.stats.analyzer.dal.entity.Similarity;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

@UtilityClass
public class SimilarityMapper {
    public static Similarity fromAvro(EventSimilarityAvro avro) {
        return Similarity.builder()
                .event1(avro.getEventA())
                .event2(avro.getEventB())
                .similarity(avro.getScore())
                .timestamp(avro.getTimestamp())
                .build();
    }
}
