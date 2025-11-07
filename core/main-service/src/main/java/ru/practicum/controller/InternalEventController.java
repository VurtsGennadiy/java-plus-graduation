package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.interaction.client.EventClient;
import ru.practicum.interaction.dto.EventFullDto;
import ru.practicum.service.EventService;

/**
 * Внутренний контроллер, необходимый для взаимодействия микросервисов
 */

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalEventController implements EventClient {
    private final EventService eventService;

    /**
     * Получить информацию о событии.
     * Метод вызывается из микросервиса Participation.
     */
    @GetMapping("/events/{eventId}")
    public EventFullDto getEventForParticipationService(@PathVariable Long eventId) {
        return eventService.getEventByIdForParticipation(eventId);
    }
}
