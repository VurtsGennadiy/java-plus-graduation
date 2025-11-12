package ru.practicum.interaction.client;

import jakarta.validation.constraints.Positive;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.interaction.client.fallback.RequestClientFallbackFactory;
import ru.practicum.interaction.dto.participation.ConfirmingParticipationRequest;
import ru.practicum.interaction.dto.participation.EventRequestStatusUpdateResult;
import ru.practicum.interaction.dto.participation.ParticipationRequestDto;

import java.util.List;
import java.util.Map;

/**
 * Клиент сервиса Request для работы с запросами от авторов событий и администраторов
 */
@FeignClient(name = "request-service", fallbackFactory = RequestClientFallbackFactory.class)
public interface RequestClient {

    /**
     * Подтверждение или отклонение заявок на участие в событии.
     * Вызывается из микросервиса events.
     */
    @PatchMapping
    EventRequestStatusUpdateResult confirmingRequests(@RequestBody ConfirmingParticipationRequest request);

    /**
     * Получение информации о заявках на участие в событии
     */
    @GetMapping("/event/{eventId}")
    List<ParticipationRequestDto> getRequestForEvent(@PathVariable @Positive Long eventId);

    /**
     * Получение информации о количестве принятых заявок на участие
     */
    @GetMapping("/confirmed")
    Map<Long, Long> getConfirmedRequestsCount(@RequestParam List<Long> eventIds);
}
