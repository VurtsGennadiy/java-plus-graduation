package ru.practicum.participation.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.interaction.client.ParticipationInitiatorClient;
import ru.practicum.interaction.dto.participation.ConfirmingParticipationRequest;
import ru.practicum.interaction.dto.participation.EventRequestStatusUpdateResult;
import ru.practicum.interaction.dto.participation.ParticipationRequestDto;
import ru.practicum.participation.service.ParticipationRequestService;

import java.util.List;
import java.util.Map;

/**
 * Контроллер для работы с запросами авторов событий
 */
@Validated
@RestController
@RequiredArgsConstructor
public class InitiatorParticipationRequestController implements ParticipationInitiatorClient {
    private final ParticipationRequestService participationRequestService;

    /**
     * Подтверждение или отклонение заявок на участие в событии.
     * Вызывается из микросервиса events.
     */
    @Override
    @PatchMapping
    public EventRequestStatusUpdateResult confirmingRequests(@RequestBody ConfirmingParticipationRequest request) {
        return participationRequestService.confirmingRequests(request);
    }

    /**
     * Получение информации о заявках на участие в событии
     */
    @Override
    @GetMapping("/event/{eventId}")
    public List<ParticipationRequestDto> getRequestForEvent(@PathVariable @Positive Long eventId) {
        return participationRequestService.getRequestForEvent(eventId);
    }

    /**
     * Получение информации о количестве принятых заявок на участие
     */
    @Override
    @GetMapping("/confirmed")
    public Map<Long, Long> getConfirmedRequestsCount(@RequestParam List<Long> eventIds) {
        return participationRequestService.getConfirmedRequestsCount(eventIds);
    }
}
