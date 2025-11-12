package ru.practicum.controller.privateAPI;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.interaction.dto.event.EventFullDto;
import ru.practicum.interaction.dto.event.EventShortDto;
import ru.practicum.dto.event.NewEventDto;
import ru.practicum.dto.event.UpdateEventUserRequest;
import ru.practicum.interaction.dto.participation.EventRequestStatusUpdateRequest;
import ru.practicum.interaction.dto.participation.EventRequestStatusUpdateResult;
import ru.practicum.interaction.dto.participation.ParticipationRequestDto;
import ru.practicum.interaction.params.EventUserSearchParam;
import ru.practicum.service.EventService;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}/events")
public class PrivateEventController {
    private final EventService eventService;
    private final String id = "/{eventId}";
    private final String requests = "/{eventId}/requests";

    /**
     * Получение событий, добавленных текущим пользователем
     */
    @GetMapping
    public List<EventShortDto> getUsersEvents(@PathVariable @Positive Long userId,
                                              @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
                                              @RequestParam(defaultValue = "10") @Positive Integer size) {

        EventUserSearchParam params = EventUserSearchParam.builder()
                .userId(userId)
                .from(from)
                .size(size)
                .build();

        return eventService.getUsersEvents(params);
    }

    /**
     * Добавление нового события
     */
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public EventFullDto createEvent(@PathVariable @Positive Long userId,
                                    @RequestBody @Valid NewEventDto dto) {

        return eventService.saveEvent(dto, userId);
    }

    /**
     * Получение полной информации о событии добавленном текущим пользователем
     */
    @GetMapping(id)
    public EventFullDto getEventByUserIdAndEventId(@PathVariable @Positive Long userId,
                                                   @PathVariable @Positive Long eventId) {

        return eventService.getEventByIdAndUserId(eventId, userId);
    }

    /**
     * Изменение события добавленного текущим пользователем
     */
    @PatchMapping(id)
    public EventFullDto updateEventByUser(@PathVariable @Positive Long userId,
                                          @PathVariable @Positive Long eventId,
                                          @RequestBody @Valid UpdateEventUserRequest request) {

        return eventService.updateEventByUser(eventId, userId, request);
    }

    /**
     * Получение информации о запросах на участие в событии текущего пользователя
     */
    @GetMapping(requests)
    public List<ParticipationRequestDto> getUsersRequests(@PathVariable @Positive Long userId,
                                                          @PathVariable @Positive Long eventId) {
        return eventService.getParticipationRequestForUserEvent(userId, eventId);
    }

    /**
     * Изменение статуса (подтверждена, отменена) заявок на участие в событии текущего пользователя
     */
    @PatchMapping(requests)
    public EventRequestStatusUpdateResult confirmingParticipationRequests(@PathVariable @Positive Long userId,
                                                                          @PathVariable @Positive Long eventId,
                                                                          @RequestBody EventRequestStatusUpdateRequest request) {
        return eventService.confirmingParticipationRequests(userId, eventId, request);
    }
}


