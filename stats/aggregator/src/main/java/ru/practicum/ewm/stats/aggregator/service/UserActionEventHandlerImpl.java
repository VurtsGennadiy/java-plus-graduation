package ru.practicum.ewm.stats.aggregator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.*;

/**
 * Реализация обработчика действий пользователя.
 * Используется хранение данных в памяти программы.
 * Расчёт производится на основе косинусного коэффициента (коэффициент Отиаи):
 *                      Smin(A, B)
 *  similarity(A, B) = ----------------
 *                      sqrt(S(A)) * sqrt(S(B))
 *  Числитель - сумма минимальных весов для двух мероприятий A и B
 *  Знаменатель - произведение квадратных корней от общих сумм весов для каждого мероприятия.
 */
@Service
@Slf4j
public class UserActionEventHandlerImpl implements UserActionEventHandler {
    /**
     * Весовые коэффициенты для типов взаимодействий
     */
    private final Map<ActionTypeAvro, Double> interactionWeights = new HashMap<>();

    /**
     * Идентификаторы всех событий
     */
    private final Set<Long> events = new HashSet<>();

    /**
     * Матрица весов действий пользователей с мероприятиями.
     * Ключ - id мероприятия
     * Значение - отображение, где ключ - id пользователя, значение - максимальный вес из всех его действий с этим мероприятием
     */
    private final Map<Long, Map<Long, Double>> maxWeights = new HashMap<>();

    /**
     * Суммы весов каждого мероприятия
     * Ключ - id мероприятия
     * Значение - сумма весов действий пользователя с ним
     */
    private final Map<Long, Double> eventWeightsSums = new HashMap<>();

    /**
     * Сумма минимальных весов для каждой пары мероприятий
     * Ключ - id мероприятия
     * Значение - отображение, где ключ - второе мероприятие, значение - сумма их минимальных весов
     * Для избежания дублирования для каждой пары событий хранится только одна запись и идентификаторы событий упорядочены в числовом порядке.
     */
    private final Map<Long, Map<Long, Double>> pairMinWeightsSums = new HashMap<>();

    public UserActionEventHandlerImpl() {
        interactionWeights.put(ActionTypeAvro.VIEW, 0.4);
        interactionWeights.put(ActionTypeAvro.REGISTER, 0.8);
        interactionWeights.put(ActionTypeAvro.LIKE, 1.0);
    }

    @Override
    public Map<Long, Map<Long, Double>> handle(UserActionAvro userAction) {
        log.debug("Обработка события действия пользователя: {}", userAction);

        Long event = userAction.getEventId();
        Long user = userAction.getUserId();
        Double newWeight = interactionWeights.get(userAction.getActionType());
        Double oldWeight = maxWeights
                .computeIfAbsent(event, k -> new HashMap<>())
                .computeIfAbsent(user,  k -> 0.0);

        if (newWeight <= oldWeight) {
            log.debug("Вес взаимодействия пользователя {} с мероприятием {} не изменился, пересчёт схожести мероприятий не требуется",
                    user, event);
            return Collections.emptyMap();
        } else {
            // не обновляю мапу maxWeigts в этом месте, так как старое значение еще нужно в расчётах
            log.debug("Вес взаимодействия пользователя {} с мероприятием {} изменён с {} на {}, нужен пересчёт схожести мероприятий",
                    user, event, oldWeight, newWeight);
        }

        if (!events.contains(userAction.getEventId())) {
            log.trace("Для мероприятия {} ещё не было взаимодействий", event);
            return calculateSimilaritiesForNewEvent(userAction);
        }

        return updateSimilarities(userAction);
    }

    /**
     * Пересчёт значений схожести мероприятий
     */
    private Map<Long, Map<Long, Double>> updateSimilarities(UserActionAvro userAction) {
        Long event = userAction.getEventId();
        Long user = userAction.getUserId();

        Double newWeight = interactionWeights.get(userAction.getActionType());
        Double oldWeight = maxWeights.get(event).get(user);

        eventWeightsSums.put(event, eventWeightsSums.get(event) + newWeight - oldWeight);

        Map<Long, Map<Long, Double>> updatedSimilarities = new HashMap<>();
        for (Long otherEvent : events) {
            if (otherEvent.equals(event)) {
                continue;
            }

            log.trace("Расчёт схожести для событий {} и {}", event, otherEvent);
            if (!checkUserHasInteractWithEvent(user, otherEvent)) {
                log.trace("Пользователь {} не взаимодействовал с событием {}, пересчёт схожести не требуется", user, otherEvent);
                continue;
            }

            // обновление суммы минимальных весов
            Double otherWeight = maxWeights.get(otherEvent).get(user);
            double oldMin = Math.min(oldWeight, otherWeight);
            double newMin = Math.min(newWeight, otherWeight);
            Double pairMinWeightsSum = getFromEventOrderedMap(event, otherEvent, pairMinWeightsSums) - oldMin + newMin;
            putToEventOrderedMap(event, otherEvent, pairMinWeightsSum, pairMinWeightsSums);

            Double similarity = calculateSimilarity(event, otherEvent);
            putToEventOrderedMap(event, otherEvent, similarity, updatedSimilarities);
            log.debug("Рассчитанный коэффициент схожести между событиями {} и {} равен {}", event, otherEvent, similarity);
        }
        maxWeights.get(event).put(user, newWeight);
        return updatedSimilarities;
    }

    /**
     * Расчёт схожести нового мероприятия (с которым никто прежде не взаимодействовал) со всеми остальными.
     */
    private Map<Long, Map<Long, Double>> calculateSimilaritiesForNewEvent(UserActionAvro userAction) {
        Long user =  userAction.getUserId();
        Long event = userAction.getEventId();
        log.trace("Расчёт коэффициентов схожести событий для нового мероприятия {}", event);

        Double weight = interactionWeights.get(userAction.getActionType());
        maxWeights.get(event).put(user, weight);
        eventWeightsSums.put(event, weight);

        Map<Long, Map<Long, Double>> similarities = new HashMap<>();
        for (Long otherEvent : events) {
            log.trace("Расчёт схожести для событий {} и {}", event, otherEvent);
            if (!checkUserHasInteractWithEvent(user, otherEvent)) {
                log.trace("Пользователь {} не взаимодействовал с событием {}, пересчёт схожести не требуется", user, otherEvent);
                continue;
            }

            Double otherWeight = maxWeights.get(otherEvent).get(user);
            putToEventOrderedMap(event, otherEvent, Math.min(weight, otherWeight), pairMinWeightsSums);

            Double similarity = calculateSimilarity(event, otherEvent);
            putToEventOrderedMap(event, otherEvent, similarity, similarities);
            log.debug("Рассчитанный коэффициент схожести между событиями {} и {} равен {}", event, otherEvent, similarity);
        }

        events.add(event);
        return similarities;
    }

    /**
     * Расчёт сходства двух событий
     */
    private Double calculateSimilarity(Long eventA, Long eventB) {
        Double sumMinWeights = getFromEventOrderedMap(eventA, eventB, pairMinWeightsSums);
        Double eventAWeightsSum = eventWeightsSums.get(eventA);
        Double eventBWeightsSum = eventWeightsSums.get(eventB);
        return sumMinWeights / (Math.sqrt(eventAWeightsSum) * Math.sqrt(eventBWeightsSum));
    }

    /**
     * Добавить запись в мапу с упорядоченными id событий
     */
    private void putToEventOrderedMap(Long eventA, Long eventB, Double sum, Map<Long, Map<Long, Double>> orderedMap) {
        Long first  = Math.min(eventA, eventB);
        Long second = Math.max(eventA, eventB);

        orderedMap
                .computeIfAbsent(first, e -> new HashMap<>())
                .put(second, sum);
    }

    /**
     * Получение значение из мапы с упорядоченными id событий
     */
    private Double getFromEventOrderedMap(Long eventA, Long eventB, Map<Long, Map<Long, Double>> orderedMap) {
        Long first  = Math.min(eventA, eventB);
        Long second = Math.max(eventA, eventB);

        return orderedMap
                .computeIfAbsent(first, e -> new HashMap<>())
                .getOrDefault(second, 0.0);
    }

    /**
     * Проверяет было ли у пользователя взаимодействие с событием
     */
    private boolean checkUserHasInteractWithEvent(Long userId, Long eventId) {
        Map<Long, Double> eventWeights = maxWeights.computeIfAbsent(eventId, k -> new HashMap<>());
        return eventWeights.containsKey(userId);
    }
}
