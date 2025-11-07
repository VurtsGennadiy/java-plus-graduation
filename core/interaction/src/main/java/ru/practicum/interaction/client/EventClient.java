package ru.practicum.interaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.interaction.dto.EventFullDto;

@FeignClient(name = "main-service", path = "/internal")
public interface EventClient {

    /**
     * Получить информацию о событии.
     * Метод вызывается из микросервиса Participation.
     */
    @GetMapping("/events/{eventId}")
    EventFullDto getEventForParticipationService(@PathVariable Long eventId);
}
