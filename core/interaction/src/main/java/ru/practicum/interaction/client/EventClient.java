package ru.practicum.interaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.interaction.dto.event.EventFullDto;
import ru.practicum.interaction.dto.event.EventShortDto;
import ru.practicum.interaction.params.SortSearchParam;

import java.time.LocalDateTime;
import java.util.List;

@FeignClient(name = "main-service")
public interface EventClient {
    /**
     * Получить информацию о событии.
     * Метод вызывается из микросервиса Participation.
     */
    @GetMapping("/internal/events/{eventId}")
    EventFullDto getEventForParticipationService(@PathVariable Long eventId);

    /**
     * Получение событий с возможностью фильтрации
     */
    @GetMapping("/events")
    List<EventShortDto> getEvents(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) List<Long> users,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "EVENT_DATE") SortSearchParam sort,
            @RequestParam(defaultValue = "false") Boolean onlyAvailable,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size);
}
