package ru.practicum.ewm.stats.aggregator.service;

import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.Map;

/**
 * Обработчик событий действия пользователя.
 * Рассчитывает коэффициенты схожести мероприятий на основе действий пользователей.
 */
public interface UserActionEventHandler {
    /**
     * Пересчёт коэффициентов схожести мероприятий на основе действия пользователя
     * @param userAction сообщение о новом действии пользователя
     * @return пересчитанные коэффициенты схожести мероприятий.
     * Отображение, где ключ - id события, значение - отображение: где ключ - id другого события, значение - коэффициент схожести
     */
    Map<Long, Map<Long, Double>> handle(UserActionAvro userAction);
}
