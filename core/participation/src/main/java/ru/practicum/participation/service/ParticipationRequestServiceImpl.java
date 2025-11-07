package ru.practicum.participation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.interaction.client.EventClient;
import ru.practicum.interaction.client.ParticipationInitiatorClient;
import ru.practicum.interaction.dto.EventFullDto;
import ru.practicum.interaction.dto.participation.*;
import ru.practicum.interaction.exception.EntityNotFoundException;
import ru.practicum.interaction.logging.Loggable;
import ru.practicum.interaction.exception.ConflictException;
import ru.practicum.participation.dal.ParticipationRequestMapper;
import ru.practicum.participation.dal.ParticipationRequest;
import ru.practicum.participation.dal.ParticipationRequestRepository;
import ru.practicum.interaction.dto.EventState;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipationRequestServiceImpl implements ParticipationRequestService, ParticipationInitiatorClient {
    private final ParticipationRequestRepository requestRepository;
    private final ParticipationRequestMapper participationRequestMapper;
    private final EventClient eventClient;

    /**
     * Создание новой заявки на участие в событии
     */
    @Transactional
    @Loggable
    @Override
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        EventFullDto event = eventClient.getEventForParticipationService(eventId);

        // пользователь не является автором события
        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Initiator cannot request participation in their own event");
        }

        if (!EventState.PUBLISHED.equals(event.getState())) {
            throw new ConflictException("Event must be published to request participation");
        }

        // проверяем, что не достигнут лимит участников
        if (event.getParticipantLimit() != 0 &&
                requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED) >= event.getParticipantLimit()) {
            throw new ConflictException("Event participant limit reached");
        }

        if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            throw new ConflictException("This request already exists");
        }

        ParticipationRequest request = new ParticipationRequest();
        request.setRequesterId(userId);
        request.setEventId(eventId);
        request.setCreated(LocalDateTime.now());

        // автоматически подтверждаем заявку, если лимит участников не ограничен или не требуется подтверждение
        if (event.getParticipantLimit() == 0) {
            request.setStatus(RequestStatus.CONFIRMED);
        } else {
            request.setStatus(event.getRequestModeration() ? RequestStatus.PENDING : RequestStatus.CONFIRMED);
        }

        ParticipationRequestDto dto = participationRequestMapper.toDto(requestRepository.save(request));
        log.info("Participation request created {}", dto);
        return dto;
    }

    /**
     * Получить заявки пользователя на участие в чужих событиях
     * @param userId - id пользователя
     * @return - список заявок
     */
    @Override
    public List<ParticipationRequestDto> getRequestsByUser(Long userId) {
        log.debug("Get participation requests for user id = {}", userId);
        List<ParticipationRequest> requests = requestRepository.findAllByRequesterId(userId);
        return participationRequestMapper.toDto(requests);
    }

    /**
     * Получить заявки на участие в событии, автором которого является пользователь
     * @param eventId - id события
     * @return - список заявок
     */
    @Override
    public List<ParticipationRequestDto> getRequestForEvent(Long eventId) {
        log.debug("Get participation requests for event id = {}", eventId);
        List<ParticipationRequest> requests = requestRepository.findAllByEventId(eventId);
        return participationRequestMapper.toDto(requests);
    }

    /**
     * Отмена своего запроса на участие в событии
     * @param userId - id пользователя, автора запроса
     * @param requestId - id запроса на участие
     * @return - обновленный запрос
     */
    @Transactional
    @Loggable
    @Override
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Participation request", requestId.toString()));

        if (!request.getRequesterId().equals(userId)) {
            throw new ConflictException("User is not the requester");
        }

        request.setStatus(RequestStatus.CANCELED);
        requestRepository.save(request);
        log.info("Participation request id {} set status CANCELED", requestId);
        return participationRequestMapper.toDto(request);
    }

    /**
     * Подтверждение или отклонение заявок на участие в событии
     * @return - результат выполнения запроса, содержит два списка заявок: отклоненные и подтвержденные
     */
    @Transactional
    @Loggable
    @Override
    public EventRequestStatusUpdateResult confirmingRequests(ConfirmingParticipationRequest request) {
        EventFullDto event = request.getEvent();
        EventRequestStatusUpdateRequest updateRequest = request.getUpdateRequest();
        List<ParticipationRequest> requests = requestRepository.findAllById(updateRequest.getRequestIds());

        boolean hasNotPendingRequests = requests.stream().map(ParticipationRequest::getStatus)
                .anyMatch(el -> el != RequestStatus.PENDING);
        if (hasNotPendingRequests) {
            throw new ConflictException("Can't change status when request status is not PENDING");
        }

        List<ParticipationRequestDto> confirmedRequests = new ArrayList<>();
        List<ParticipationRequestDto> rejectedRequests = new ArrayList<>();
        if (RequestStatus.REJECTED.equals(updateRequest.getStatus())) {
            requests.forEach(participationRequest -> {
                participationRequest.setStatus(RequestStatus.REJECTED);
                rejectedRequests.add(participationRequestMapper.toDto(participationRequest));
            });
            log.info("Rejected participation requests: {}", requests.stream().map(ParticipationRequest::getId).toList());
        } else {
            Integer participantLimit = event.getParticipantLimit();
            int confirmedCount = requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);
            if (participantLimit - confirmedCount < requests.size()) {
                throw new ConflictException("Requests out of limit");
            }
            requests.forEach(participationRequest -> {
                participationRequest.setStatus(RequestStatus.CONFIRMED);
                confirmedRequests.add(participationRequestMapper.toDto(participationRequest));
            });
            log.info("Confirmed participation requests: {}", requests.stream().map(ParticipationRequest::getId).toList());
        }

        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult(confirmedRequests, rejectedRequests);
        requestRepository.saveAll(requests);
        return result;
    }

    @Override
    @Loggable
    public Map<Long, Long> getConfirmedRequestsCount(List<Long> eventIds) {
        return requestRepository.countRequestsByEventIdsAndStatus(eventIds, RequestStatus.CONFIRMED);
    }
}
