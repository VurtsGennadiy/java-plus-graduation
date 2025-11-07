package ru.practicum.participation.service;

import ru.practicum.interaction.dto.EventFullDto;
import ru.practicum.participation.dto.EventRequestStatusUpdateRequest;
import ru.practicum.participation.dto.EventRequestStatusUpdateResult;
import ru.practicum.participation.dto.ParticipationRequestDto;

import java.util.List;

public interface ParticipationRequestService {
    List<ParticipationRequestDto> getRequestForEventByUserId(Long eventId, Long userId);

    EventRequestStatusUpdateResult updateRequests(EventFullDto event, Long userId, EventRequestStatusUpdateRequest updateRequest);

    List<ParticipationRequestDto> getRequestsByUser(Long userId);

    ParticipationRequestDto createRequest(Long userId, Long eventId);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);
}
