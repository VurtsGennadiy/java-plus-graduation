package ru.practicum.controller.internalAPI;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.interaction.dto.event.EventFullDto;
import ru.practicum.service.EventService;

/**
 * Внутренний контроллер, необходимый для взаимодействия микросервисов
 */
@Validated
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalEventController {
    private final EventService eventService;

    /**
     * Получить информацию о событии.
     * Метод вызывается из микросервиса Participation.
     */
    @GetMapping("/events/{eventId}")
    public EventFullDto getEventForParticipationService(@PathVariable @Positive Long eventId) {
        return eventService.getEventByIdForParticipation(eventId);
    }
}
