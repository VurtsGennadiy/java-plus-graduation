package ru.practicum.controller.privateAPI;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.interaction.dto.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.event.NewEventDto;
import ru.practicum.dto.event.UpdateEventUserRequest;
import ru.practicum.participation.dto.EventRequestStatusUpdateRequest;
import ru.practicum.participation.dto.EventRequestStatusUpdateResult;
import ru.practicum.participation.dto.ParticipationRequestDto;
import ru.practicum.params.EventUserSearchParam;
import ru.practicum.service.EventService;
import ru.practicum.participation.service.ParticipationRequestService;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}/events")
public class PrivateEventController {

    private final EventService eventService;
    private final ParticipationRequestService requestService;
    private final String id = "/{eventId}";
    private final String requests = "/{eventId}/requests";


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

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public EventFullDto createEvent(@PathVariable @Positive Long userId,
                                    @RequestBody @Valid NewEventDto dto) {

        return eventService.saveEvent(dto, userId);
    }

    @GetMapping(id)
    public EventFullDto getEventByUserIdAndEventId(@PathVariable @Positive Long userId,
                                                   @PathVariable @Positive Long eventId) {

        return eventService.getEventByIdAndUserId(eventId, userId);
    }

    @PatchMapping(id)
    public EventFullDto updateEventByUser(@PathVariable @Positive Long userId,
                                          @PathVariable @Positive Long eventId,
                                          @RequestBody @Valid UpdateEventUserRequest event) {

        return eventService.updateEventByUser(eventId, userId, event);
    }

    // TODO наверное надо проверить что пользователь является автором события?
    @GetMapping(requests)
    public List<ParticipationRequestDto> getUsersRequests(@PathVariable @Positive Long userId,
                                                          @PathVariable @Positive Long eventId) {

        List<ParticipationRequestDto> requestForEventByUserId = requestService.getRequestForEventByUserId(eventId, userId);
        return requestForEventByUserId;
    }

    @PatchMapping(requests)
    public EventRequestStatusUpdateResult updateUsersRequests(@PathVariable @Positive Long userId,
                                                              @PathVariable @Positive Long eventId,
                                                              @RequestBody EventRequestStatusUpdateRequest updateRequest) {
        EventFullDto event = eventService.getEventByIdAndUserId(eventId, userId);
        return requestService.updateRequests(event, userId, updateRequest);
    }
}


