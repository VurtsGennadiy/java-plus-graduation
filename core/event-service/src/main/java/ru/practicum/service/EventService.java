package ru.practicum.service;

import ru.practicum.dto.event.*;
import ru.practicum.interaction.dto.event.EventFullDto;
import ru.practicum.interaction.dto.event.EventShortDto;
import ru.practicum.interaction.params.EventAdminSearchParam;
import ru.practicum.interaction.params.EventUserSearchParam;
import ru.practicum.interaction.params.PublicEventSearchParam;
import ru.practicum.interaction.dto.participation.EventRequestStatusUpdateRequest;
import ru.practicum.interaction.dto.participation.EventRequestStatusUpdateResult;
import ru.practicum.interaction.dto.participation.ParticipationRequestDto;

import java.util.List;


public interface EventService {

    List<EventFullDto> getEventsByParams(EventAdminSearchParam param);

    EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest request);

    EventFullDto getEventById(Long id);

    List<EventShortDto> searchEvents(PublicEventSearchParam param);

    List<EventShortDto> getUsersEvents(EventUserSearchParam param);

    EventFullDto saveEvent(NewEventDto dto, Long userId);

    EventFullDto getEventByIdAndUserId(Long userId, Long eventId);

    EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest request);

    List<ParticipationRequestDto> getParticipationRequestForUserEvent(Long eventId, Long userId);

    EventRequestStatusUpdateResult confirmingParticipationRequests(Long userId, Long eventId, EventRequestStatusUpdateRequest request);

    EventFullDto getEventByIdForParticipation(Long id);
}
