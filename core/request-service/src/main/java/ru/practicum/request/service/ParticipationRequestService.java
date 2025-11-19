package ru.practicum.request.service;

import ru.practicum.interaction.dto.participation.ConfirmingParticipationRequest;
import ru.practicum.interaction.dto.participation.EventRequestStatusUpdateResult;
import ru.practicum.interaction.dto.participation.ParticipationRequestDto;

import java.util.List;
import java.util.Map;

public interface ParticipationRequestService {

    List<ParticipationRequestDto> getRequestsByUser(Long userId);

    ParticipationRequestDto createRequest(Long userId, Long eventId);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

    List<ParticipationRequestDto> getRequestForEvent(Long eventId);

    EventRequestStatusUpdateResult confirmingRequests(ConfirmingParticipationRequest request);

    Map<Long, Long> getConfirmedRequestsCount(List<Long> eventIds);
}
