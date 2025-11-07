package ru.practicum.participation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.interaction.dto.EventFullDto;
import ru.practicum.interaction.logging.Loggable;
import ru.practicum.interaction.service.FindEvent;
import ru.practicum.participation.dto.EventRequestStatusUpdateRequest;
import ru.practicum.participation.dto.EventRequestStatusUpdateResult;
import ru.practicum.participation.dto.ParticipationRequestDto;
import ru.practicum.interaction.exception.ConflictException;
import ru.practicum.interaction.exception.NotFoundException;
import ru.practicum.participation.dto.ParticipationRequestMapper;
import ru.practicum.participation.dal.ParticipationRequest;
import ru.practicum.participation.dal.RequestStatus;
import ru.practicum.participation.dal.ParticipationRequestRepository;
import ru.practicum.interaction.dto.EventState;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipationRequestServiceImpl implements ParticipationRequestService {

    private final ParticipationRequestRepository requestRepository;
    //private final UserRepository userRepository;
    //private final EventRepository eventRepository;
    private final ParticipationRequestMapper participationRequestMapper;

    private final FindEvent findEvent;


    @Override
    public List<ParticipationRequestDto> getRequestForEventByUserId(Long eventId, Long userId) {
        log.info("Get request for event by id: {}", eventId);
/*        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event id" + eventId + "not found"));
        if (!Objects.equals(event.getInitiator().getId(), userId)) {
            throw new ConflictException("Can't get request for event id=" + eventId + "by user id=" + userId);
        }*/
        List<ParticipationRequest> requests = requestRepository.findAllByEventId(eventId);
        return requests.stream()
                .map(participationRequestMapper::toDto)
                .toList();
    }

    @Override
    public EventRequestStatusUpdateResult updateRequests(EventFullDto event,
                                                         Long userId,
                                                         EventRequestStatusUpdateRequest updateRequest) {
        log.info("Update request: {}", updateRequest);
        List<ParticipationRequest> requestList = requestRepository.findAllById(updateRequest.getRequestIds());

        updateRequests(requestList, updateRequest.getStatus(), event);

        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();
        requestList.forEach(request -> {
            switch (request.getStatus()) {
                case RequestStatus.REJECTED ->
                        result.getRejectedRequests().add(participationRequestMapper.toDto(request));
                case RequestStatus.CONFIRMED ->
                        result.getConfirmedRequests().add(participationRequestMapper.toDto(request));
            }
        });
        return result;
    }

    @Override
    public List<ParticipationRequestDto> getRequestsByUser(Long userId) {
/*        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));*/
        List<ParticipationRequest> requests = requestRepository.findAllByRequesterId(userId);
        return participationRequestMapper.toDto(requests);
    }

    @Override
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
/*        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));*/
/*        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));*/

        // костыль, заменить на Feign!
        EventFullDto event = findEvent.getEventByIdForParticipation(eventId);

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Initiator cannot request participation in their own event");
        }

        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Event must be published to request participation");
        }

        if (event.getParticipantLimit() != 0 &&
                requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED) >= event.getParticipantLimit()) {
            throw new ConflictException("Event participant limit reached");
        }

        ParticipationRequest request = new ParticipationRequest();
        request.setRequesterId(userId);
        request.setEventId(eventId);

        if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            throw new ConflictException("This request already exists");
        }

        if (event.getParticipantLimit() == 0) {
            request.setStatus(RequestStatus.CONFIRMED);
        } else {
            request.setStatus(event.getRequestModeration() ? RequestStatus.PENDING : RequestStatus.CONFIRMED);
        }
        request.setCreated(LocalDateTime.now());

        return participationRequestMapper.toDto(requestRepository.save(request));
    }

    @Override
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request not found"));

        if (!request.getRequesterId().equals(userId)) {
            throw new ConflictException("User is not the requester");
        }

        request.setStatus(RequestStatus.CANCELED);
        return participationRequestMapper.toDto(requestRepository.save(request));
    }

    private void updateRequests(List<ParticipationRequest> requests, RequestStatus status, EventFullDto event) {
        boolean hasNotPendingRequests = requests.stream().map(ParticipationRequest::getStatus)
                .anyMatch(el -> el != RequestStatus.PENDING);

        if (hasNotPendingRequests) {
            throw new ConflictException("Can't change status when request status is not PENDING");
        }

        if (status == RequestStatus.REJECTED) {
            for (ParticipationRequest request : requests) {
                request.setStatus(RequestStatus.REJECTED);
            }
            return;
        }

        Boolean requestModeration = event.getRequestModeration();
        Integer participantLimit = event.getParticipantLimit();
        if (!requestModeration && participantLimit == null) {
            requests.forEach(request -> request.setStatus(status));
            return;
        }

        int confirmed = requestRepository
                .countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);
        for (ParticipationRequest request : requests) {
            if (confirmed >= participantLimit) {
                throw new ConflictException("Requests out of limit");
            } else {
                request.setStatus(status);
                confirmed++;
            }
        }
        requestRepository.saveAll(requests);
    }

}
