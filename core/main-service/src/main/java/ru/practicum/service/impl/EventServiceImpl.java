package ru.practicum.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.StatsClient;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.dto.event.*;
import ru.practicum.entity.Category;
import ru.practicum.entity.Event;
import ru.practicum.entity.Location;
import ru.practicum.interaction.client.ParticipationInitiatorClient;
import ru.practicum.interaction.dto.EventFullDto;
import ru.practicum.interaction.dto.EventState;
import ru.practicum.interaction.dto.LocationDto;
import ru.practicum.interaction.dto.participation.*;
import ru.practicum.interaction.exception.ConflictException;
import ru.practicum.interaction.exception.EntityNotFoundException;
import ru.practicum.interaction.exception.NotFoundException;
import ru.practicum.interaction.logging.Loggable;
import ru.practicum.mapper.EventMapper;
import ru.practicum.params.EventAdminSearchParam;
import ru.practicum.params.EventUserSearchParam;
import ru.practicum.params.PublicEventSearchParam;
import ru.practicum.params.SortSearchParam;
import ru.practicum.repository.CategoryRepository;
import ru.practicum.repository.EventRepository;
import ru.practicum.service.EventService;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static ru.practicum.specifications.EventSpecifications.eventAdminSearchParamSpec;
import static ru.practicum.specifications.EventSpecifications.eventPublicSearchParamSpec;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final EventMapper eventMapper;
    private final StatsClient statsClient;
    private final ParticipationInitiatorClient participationClient;

    @Override
    public List<EventFullDto> getEventsByParams(EventAdminSearchParam params) {
        log.debug("Get events by params: {}", params);

        Page<Event> searched = eventRepository.findAll(eventAdminSearchParamSpec(params), params.getPageable());

        List<Long> eventIds = searched.stream()
                .limit(params.getSize())
                .map(Event::getId)
                .toList();

        Map<Long, Long> views = getViews(eventIds);
        Map<Long, Long> confirmed = participationClient.getConfirmedRequestsCount(eventIds);
        return searched.stream()
                .limit(params.getSize())
                .map(event -> {
                    EventFullDto dto = eventMapper.toFullDto(event);
                    dto.setConfirmedRequests(confirmed.get(dto.getId()) == null ? 0 : confirmed.get(dto.getId()));
                    dto.setViews(views.get(event.getId()) == null ? 0 : views.get(event.getId()));
                    return dto;
                })
                .collect(toList());
    }

    @Override
    @Loggable
    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest updateRequest) {
        log.info("Update event: {}", updateRequest);

        Event event = getEventByIdOrElseThrow(eventId);

        if (event.getState() != EventState.PENDING && updateRequest.getStateAction() == AdminEventAction.PUBLISH_EVENT) {
            throw new ConflictException("Cannot publish the event because it's not in the right state: " + event.getState());
        }
        if (event.getState() == EventState.PUBLISHED && updateRequest.getStateAction() == AdminEventAction.REJECT_EVENT) {
            throw new ConflictException("Cannot reject the event because it's not in the right state: PUBLISHED");
        }
        if (event.getEventDate().minusHours(1).isBefore(LocalDateTime.now())) {
            throw new ConflictException("To late to change event");
        }

        updateNotNullFields(event, updateRequest);
        event.setState(updateRequest.getStateAction() == AdminEventAction.PUBLISH_EVENT ? EventState.PUBLISHED : EventState.CANCELED);
        event.setPublishedOn(LocalDateTime.now());
        Event updated = eventRepository.save(event);
        log.info("Event {} are updated by Admin", eventId);

        EventFullDto dto = eventMapper.toFullDto(updated);
        Map<Long, Long> views = getViews(List.of(eventId));
        Map<Long, Long> confirmedRequests = participationClient.getConfirmedRequestsCount(List.of(eventId));
        dto.setViews(views.get(eventId));
        dto.setConfirmedRequests(confirmedRequests.get(eventId));
        return dto;
    }

    @Override
    public EventFullDto getEventById(Long id) {
        log.info("Get event: {}", id);

        Event event = eventRepository.findByIdAndState(id, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Событие не найдено или не опубликовано"));
        Map<Long, Long> confirmed = participationClient.getConfirmedRequestsCount(List.of(id));
        Map<Long, Long> views = getViews(List.of(event.getId()));

        EventFullDto dto = eventMapper.toFullDto(event);
        dto.setConfirmedRequests(confirmed.get(dto.getId()));
        dto.setViews(views.get(dto.getId()));
        return dto;
    }

    @Override
    public List<EventShortDto> searchEvents(PublicEventSearchParam param) {
        log.info("Search events: {}", param);

        Page<Event> events = eventRepository.findAll(eventPublicSearchParamSpec(param), param.getPageable());

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .toList();
        Map<Long, Long> views = getViews(eventIds);
        Map<Long, Long> confirmed = participationClient.getConfirmedRequestsCount(eventIds);

        Stream<EventShortDto> eventShortDtoStream = events.stream()
                .map(event -> {
                    if (param.getOnlyAvailable() && confirmed.get(event.getId()) >= event.getParticipantLimit()) {
                        return null;
                    }
                    EventShortDto dto = eventMapper.toShortDto(event);
                    dto.setConfirmedRequests(confirmed.get(dto.getId()) == null ? 0 : confirmed.get(dto.getId()));
                    dto.setViews(views.get(event.getId()) == null ? 0 : views.get(dto.getId()));
                    return dto;
                })
                .filter(Objects::nonNull);
        if (param.getSort() == SortSearchParam.VIEWS) {
            return eventShortDtoStream
                    .sorted(Comparator.comparingLong(EventShortDto::getViews))
                    .toList();
        } else {
            return eventShortDtoStream
                    .toList();
        }
    }

    @Override
    public List<EventShortDto> getUsersEvents(EventUserSearchParam param) {
        log.info("Get users events: {}", param);
        Page<Event> events = eventRepository.findByInitiator(param.getUserId(), param.getPageable());

        List<Long> eventIds = events.stream().map(Event::getId).toList();
        Map<Long, Long> views = getViews(eventIds);
        Map<Long, Long> confirmedRequests = participationClient.getConfirmedRequestsCount(eventIds);

        return events.stream()
                .map(event -> {
                    EventShortDto shortDto = eventMapper.toShortDto(event);
                    shortDto.setViews(views.get(event.getId()));
                    shortDto.setConfirmedRequests(confirmedRequests.get(event.getId()));
                    return shortDto;
                })
                .toList();

    }

    @Override
    @Transactional
    @Loggable
    public EventFullDto saveEvent(NewEventDto dto, Long userId) {
        Long categoryId = dto.getCategory();
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Категория с id=" + categoryId + " не найдена"));

        Event event = eventMapper.toEntity(dto, userId);
        event.setInitiator(userId);
        event.setCategory(category);

        Event saved = eventRepository.saveAndFlush(event);
        log.info("Создано новое событие id={}", saved.getId());

        EventFullDto dtoResponse = eventMapper.toFullDto(saved);
        dtoResponse.setViews(0L);
        dtoResponse.setConfirmedRequests(0L);
        return dtoResponse;
    }

    @Override
    @Loggable
    public EventFullDto getEventByIdAndUserId(Long eventId, Long userId) {
        Event event = getEventByIdOrElseThrow(eventId);
        checkUserIsEventInitiator(event, userId);

        Map<Long, Long> confirmed = participationClient.getConfirmedRequestsCount(List.of(eventId));
        Map<Long, Long> views = getViews(List.of(event.getId()));

        EventFullDto dto = eventMapper.toFullDto(event);
        dto.setConfirmedRequests(confirmed.get(dto.getId()));
        dto.setViews(views.get(dto.getId()));
        return dto;
    }

    @Override
    @Loggable
    @Transactional
    public EventFullDto updateEventByUser(Long eventId, Long userId, UpdateEventUserRequest request) {
        Event eventToUpdate = getEventByIdOrElseThrow(eventId);
        checkUserIsEventInitiator(eventToUpdate, userId);
        if (EventState.PUBLISHED.equals(eventToUpdate.getState())) {
            throw new ConflictException("Событие в статусе PUBLISHED недоступно для редактирования");
        }

        updateNotNullFields(eventToUpdate, request);
        if (request.getStateAction() == UserEventAction.CANCEL_REVIEW) {
            eventToUpdate.setState(EventState.CANCELED);
        } else if (request.getStateAction() == UserEventAction.SEND_TO_REVIEW) {
            eventToUpdate.setState(EventState.PENDING);
        }
        Event updated = eventRepository.save(eventToUpdate);
        log.info("Event {} are updated by author", eventId);

        Map<Long, Long> confirmed = participationClient.getConfirmedRequestsCount(List.of(eventId));
        Map<Long, Long> views = getViews(List.of(eventId));

        EventFullDto result = eventMapper.toFullDto(updated);
        result.setConfirmedRequests(confirmed.get(eventId));
        result.setViews(views.get(eventId));
        return result;
    }

    private Map<Long, Long> getViews(List<Long> eventIds) {
        List<ViewStatsDto> stats = statsClient.getStats(
                "2000-01-01 00:00:00",
                "2100-01-01 00:00:00",
                eventIds.stream().map(id -> "/events/" + id).toList(),
                true);
        return stats.stream()
                .filter(statsDto -> !statsDto.getUri().equals("/events"))
                .collect(toMap(statDto ->
                        Long.parseLong(statDto.getUri().replace("/events/", "")), ViewStatsDto::getHits));
    }

    private void updateNotNullFields(Event eventToUpdate, UpdateEventUserRequest event) {
        if (event.getAnnotation() != null) eventToUpdate.setAnnotation(event.getAnnotation());
        if (event.getCategory() != null) eventToUpdate.setCategory(Category.builder().id(event.getCategory()).build());
        if (event.getDescription() != null) eventToUpdate.setDescription(event.getDescription());
        if (event.getEventDate() != null) eventToUpdate.setEventDate(event.getEventDate());
        if (event.getLocation() != null) {
            LocationDto locDto = event.getLocation();
            Location loc = new Location();
            loc.setLat(locDto.getLat());
            loc.setLon(locDto.getLon());
            eventToUpdate.setLocation(loc);
        }
        if (event.getPaid() != null) eventToUpdate.setPaid(event.getPaid());
        if (event.getParticipantLimit() != null) eventToUpdate.setParticipantLimit(event.getParticipantLimit());
        if (event.getRequestModeration() != null) eventToUpdate.setRequestModeration(event.getRequestModeration());
        if (event.getTitle() != null) eventToUpdate.setTitle(event.getTitle());
    }

    private void updateNotNullFields(Event eventToUpdate, UpdateEventAdminRequest event) {
        if (event.getAnnotation() != null) eventToUpdate.setAnnotation(event.getAnnotation());
        if (event.getCategory() != null) eventToUpdate.setCategory(Category.builder().id(event.getCategory()).build());
        if (event.getDescription() != null) eventToUpdate.setDescription(event.getDescription());
        if (event.getEventDate() != null) eventToUpdate.setEventDate(event.getEventDate());
        if (event.getLocation() != null) {
            LocationDto locDto = event.getLocation();
            Location loc = new Location();
            loc.setLat(locDto.getLat());
            loc.setLon(locDto.getLon());
            eventToUpdate.setLocation(loc);
        }
        if (event.getPaid() != null) eventToUpdate.setPaid(event.getPaid());
        if (event.getParticipantLimit() != null) eventToUpdate.setParticipantLimit(event.getParticipantLimit());
        if (event.getRequestModeration() != null) eventToUpdate.setRequestModeration(event.getRequestModeration());
        if (event.getTitle() != null) eventToUpdate.setTitle(event.getTitle());
    }

    @Loggable
    @Override
    public EventFullDto getEventByIdForParticipation(Long id) {
        Event event = getEventByIdOrElseThrow(id);
        return eventMapper.toFullDto(event);
    }

    /**
     * Получить все запросы пользователей на участие в событии для автора события
     * @param eventId - id события
     * @param userId - id пользователя автора события
     * @return - список заявок
     */
    @Loggable
    @Override
    public List<ParticipationRequestDto> getParticipationRequestForUserEvent(Long userId, Long eventId) {
        Event event = getEventByIdOrElseThrow(eventId);
        checkUserIsEventInitiator(event, userId);
        return participationClient.getRequestForEvent(eventId);
    }

    /**
     * Подтверждение или отклонение заявок на участие в событии
     * @param userId - id пользователя, автора события
     * @param eventId - id события
     * @param request - EventRequestStatusUpdateRequest запрос на обновление статусов
     * @return - результат выполнения запроса, содержит два списка заявок: отклоненные и подтвержденные
     */
    @Loggable
    @Override
    public EventRequestStatusUpdateResult confirmingParticipationRequests(Long userId, Long eventId, EventRequestStatusUpdateRequest request) {
        Event event = getEventByIdOrElseThrow(eventId);
        checkUserIsEventInitiator(event, userId);
        ConfirmingParticipationRequest confirmingRequest = new ConfirmingParticipationRequest();
        confirmingRequest.setEvent(eventMapper.toFullDto(event));
        confirmingRequest.setUpdateRequest(request);
        return participationClient.confirmingRequests(confirmingRequest);
    }

    private Event getEventByIdOrElseThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event", eventId.toString()));
    }

    private void checkUserIsEventInitiator(Event event, Long userId) {
        if (!event.getInitiator().equals(userId)) {
            throw new ConflictException(String.format("User %s is not initiator event %s", userId, event.getId()));
        }
    }
}