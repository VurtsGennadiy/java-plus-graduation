package ru.practicum.event.controller.publicAPI;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.service.EventService;
import ru.practicum.interaction.dto.event.EventFullDto;
import ru.practicum.interaction.dto.event.EventShortDto;
import ru.practicum.interaction.exception.BadRequestException;
import ru.practicum.interaction.params.PublicEventSearchParam;
import ru.practicum.interaction.params.SortSearchParam;
import ru.practicum.stats.CollectorClient;

import java.time.LocalDateTime;
import java.util.List;

@Validated
@Slf4j
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class PublicEventController {

    private final EventService eventService;
    private final CollectorClient collectorClient;
    private final String dateTimePattern = "yyyy-MM-dd HH:mm:ss";
    private static final String USER_ID_HEADER = "X-EWM-USER-ID";

    @GetMapping
    public List<EventShortDto> getEvents(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) List<Long> users,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) @DateTimeFormat(pattern = dateTimePattern) LocalDateTime rangeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = dateTimePattern) LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "false") Boolean onlyAvailable,
            @RequestParam(defaultValue = "EVENT_DATE") SortSearchParam sort,
            @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam(defaultValue = "10") @Positive Integer size) {

        if (rangeEnd == null) {
            rangeStart = LocalDateTime.now();
        }

        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new BadRequestException("rangeEnd can't before rangeStart");
        }

        PublicEventSearchParam param = PublicEventSearchParam.builder()
                .text(text)
                .categories(categories)
                .users(users)
                .paid(paid)
                .onlyAvailable(onlyAvailable)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .sort(sort)
                .from(from)
                .size(size)
                .build();

        return eventService.searchEvents(param);
    }

    @GetMapping("/{id}")
    public EventFullDto getEventById(@PathVariable @Positive Long id,
                                     @RequestHeader(USER_ID_HEADER) @Positive Long userId) {
        EventFullDto event = eventService.getEventById(id);
        collectorClient.saveView(userId, id);
        return event;
    }

    /**
     * Получение списка рекомендуемых мероприятий
     */
    @GetMapping("/recommendations")
    public List<EventShortDto> getRecommendations(@RequestHeader(USER_ID_HEADER) @Positive Long userId) {
        return eventService.getRecommendedEvents(userId, 100);
    }

    /**
     * Получение списка похожих мероприятий
     */
    @GetMapping("{eventId}/similar")
    public List<EventShortDto> getSimilar(@RequestHeader(USER_ID_HEADER) @Positive Long userId,
                                          @PathVariable @Positive Long eventId) {
        return eventService.getSimilarEvents(eventId, userId, 100);
    }

    /**
     * Поставить лайк мероприятию.
     * Можно поставить лайк только посещённому мероприятию.
     */
    @PutMapping("/{eventId}/like")
    public void likeEvent(@PathVariable @Positive Long eventId,
                          @RequestHeader(USER_ID_HEADER) @Positive Long userId) {

        eventService.likeEvent(eventId, userId);
        collectorClient.saveLike(userId, eventId);
    }
}
