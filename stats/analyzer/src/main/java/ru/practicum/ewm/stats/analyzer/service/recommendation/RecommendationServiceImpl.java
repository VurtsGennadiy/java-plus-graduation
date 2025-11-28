package ru.practicum.ewm.stats.analyzer.service.recommendation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.analyzer.dal.entity.Interaction;
import ru.practicum.ewm.stats.analyzer.dal.entity.Similarity;
import ru.practicum.ewm.stats.analyzer.dal.repository.InteractionRepository;
import ru.practicum.ewm.stats.analyzer.dal.repository.SimilarityRepository;
import ru.practicum.ewm.stats.proto.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.proto.UserPredictionsRequestProto;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationServiceImpl implements RecommendationService {
    private final SimilarityRepository similarityRepository;
    private final InteractionRepository interactionRepository;

    /**
     * Получить рекомендации мероприятий, основанные на предсказании оценки
     */
    @Override
    public List<RecommendedEventProto> getRecommendationsForUser(UserPredictionsRequestProto request) {
        log.debug("Запрос на получение рекомендаций {}", request);

        // ----- 1 этап

        int limit = request.getMaxResults();
        // мероприятия с которыми пользователь недавно взаимодействовал
        //List<Long> lastInteractionEvents = interactionRepository.getLastInteractionsEvents(request.getUserId(), limit);

        // все мероприятия с которыми взаимодействовал пользователь
        Set<Long> allInteractionEvents = interactionRepository.getInteractionsEvents(request.getUserId());
        if (allInteractionEvents.isEmpty()) {
            return Collections.emptyList();
        }

        // мероприятия с которыми пользователь недавно взаимодействовал
        Set<Long> lastInteractionEvents = allInteractionEvents.stream()
                .limit(limit)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // схожести для мероприятий с которыми пользователь недавно взаимодействовал
        List<Similarity> similaritiesLastInteractions = similarityRepository.findByEvent(lastInteractionEvents);

        // мероприятия с которыми не было взаимодействия, но максимально схожие с недавними взаимодействиями
        Set<Long> recommendedEvents = similaritiesLastInteractions.stream().filter(s -> {
                    Long otherEvent = lastInteractionEvents.contains(s.getEvent1()) ? s.getEvent2() : s.getEvent1();
                    return !allInteractionEvents.contains(otherEvent);
                }).limit(limit)
                .map(s -> lastInteractionEvents.contains(s.getEvent1()) ? s.getEvent2() : s.getEvent1())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // ----- 2 этап -----
        // вычисление оценки для каждого нового мероприятия

        // схожести для мероприятий с которыми не было взаимодействий
        List<Similarity> similaritiesNoInteraction = similarityRepository.findByEvent(recommendedEvents);

        List<RecommendedEventProto> result = new ArrayList<>(recommendedEvents.size());
        for (Long recommendedEvent : recommendedEvents) {
            // выгрузить мероприятия похожие на заданное, но с которыми взаимодействовал пользователь
            Map<Long, Similarity> similarEventsWithInteractions = findSimilarityEvents(recommendedEvent).stream()
                    .filter(s -> allInteractionEvents.contains(s.getEvent2()))
                    .collect(Collectors.toMap(Similarity::getEvent2, Function.identity()));

            // получить оценки пользователя
            Map<Long, Interaction> interactions = interactionRepository.findByEventIdIn(similarEventsWithInteractions.keySet())
                    .stream()
                    .collect(Collectors.toMap(Interaction::getEventId, Function.identity()));

            double weightedEstimatesSum = 0.0; // сумма взвешенных оценок
            double similarityCoefficientsSum = 0.0; // сумма коэффициентов подобия
            for (Long event : similarEventsWithInteractions.keySet()) {
                Double similarityCoefficient = similarEventsWithInteractions.get(event).getSimilarity();
                similarityCoefficientsSum += similarityCoefficient;
                weightedEstimatesSum += interactions.get(event).getRating() * similarityCoefficient;
            }
            double score = weightedEstimatesSum / similarityCoefficientsSum; // предсказанная оценка
            result.add(RecommendedEventProto.newBuilder()
                    .setEventId(recommendedEvent)
                    .setScore(score)
                    .build());
        }

        log.debug("Рекомендованные мероприятия: {}", result);
        return result;
    }

    /**
     * Получить мероприятия, похожие на заданное
     */
    @Override
    public List<RecommendedEventProto> getSimilarEvents(SimilarEventsRequestProto request) {
        log.debug("Запрос на получение похожих мероприятий: {}", request);

        List<Long> viewedEvents = interactionRepository.getViewedByUserId(request.getUserId());
        List<Similarity> allPairs = similarityRepository.findByEvent(request.getEventId());

        List<RecommendedEventProto> recommended = allPairs.stream()
                .filter(pair -> {
                    Long otherEvent = pair.getEvent1().equals(request.getEventId()) ? pair.getEvent2() : pair.getEvent1();
                    return !viewedEvents.contains(otherEvent);
                })
                .sorted()
                .limit(request.getMaxResults())
                .map(pair -> {
                    Long otherEvent = pair.getEvent1().equals(request.getEventId()) ? pair.getEvent2() : pair.getEvent1();
                    return RecommendedEventProto.newBuilder()
                            .setEventId(otherEvent)
                            .setScore(pair.getSimilarity())
                            .build();
                })
                .toList();

        log.debug("Список похожих мероприятий: {}", recommended);
        return recommended;
    }

    /**
     * Получить рейтинг мероприятий
     */
    @Override
    public List<RecommendedEventProto> getEventsRating(InteractionsCountRequestProto request) {
        log.debug("Запрос на получение рейтинга событий: {}", request);
        Map<Long, Double> ratings = new HashMap<>(request.getEventIdCount());

        List<Interaction> interactions = interactionRepository.findByEventIdIn(request.getEventIdList());
        for (Interaction interaction : interactions) {
            Long event = interaction.getEventId();
            ratings.compute(event, (k, v) -> v == null ? interaction.getRating() : v + interaction.getRating());
        }

        List<RecommendedEventProto> result = ratings.entrySet().stream()
                .map(entry -> RecommendedEventProto.newBuilder()
                        .setEventId(entry.getKey())
                        .setScore(entry.getValue())
                        .build())
                .toList();
        log.debug("Рейтинг событий: {}", result);
        return result;
    }

    /**
     * Получить все коэффициенты схожести для заданного мероприятия
     */
    private List<Similarity> findSimilarityEvents(Long targetEvent) {

        List<Similarity> similarities = similarityRepository.findByEvent(targetEvent);

        // приводим все элементы к такому виду: event1 = targetEvent
        return similarities.stream()
                .map(s -> {
                    if (Objects.equals(s.getEvent2(), targetEvent)) {
                        s.setEvent2(s.getEvent1());
                        s.setEvent1(targetEvent);
                    }
                    return s;
                }).toList();
    }
}
