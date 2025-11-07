package ru.practicum.interaction.service;

import ru.practicum.interaction.dto.EventFullDto;

/**
 * Костыль, нужный для того чтобы, избежать циклической зависимости между модулями main и participation.
 * Впоследствии заменю на Feign
 */
public interface FindEvent {

    EventFullDto getEventByIdForParticipation(Long id);
}
