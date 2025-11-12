package ru.practicum.controller.publicAPI;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import ru.practicum.StatsClient;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.interaction.dto.event.EventFullDto;
import ru.practicum.interaction.dto.event.EventShortDto;
import ru.practicum.interaction.exception.BadRequestException;
import ru.practicum.interaction.params.PublicEventSearchParam;
import ru.practicum.interaction.params.SortSearchParam;
import ru.practicum.service.EventService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class PublicEventController {

    private final EventService eventService;
    private final StatsClient statsClient;
    private final String dateTimePattern = "yyyy-MM-dd HH:mm:ss";

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
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size,
            HttpServletRequest request) {

        if (rangeEnd == null) {
            rangeStart = LocalDateTime.now();
        }

        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new BadRequestException("rangeEnd can't before rangeStart");
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern(dateTimePattern));

        statsClient.postHit(EndpointHitDto.builder()
                .app("ewm-main-service")
                .uri(request.getRequestURI())
                .ip(request.getRemoteAddr())
                .timestamp(timestamp)
                .build());

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
    public EventFullDto getEventById(@PathVariable Long id, HttpServletRequest request) {

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern(dateTimePattern));

        statsClient.postHit(EndpointHitDto.builder()
                .app("ewm-main-service")
                .uri(request.getRequestURI())
                .ip(request.getRemoteAddr())
                .timestamp(timestamp)
                .build());

        return eventService.getEventById(id);
    }
}
