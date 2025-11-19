package ru.practicum.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.StatsClient;
import ru.practicum.dal.entity.Category;
import ru.practicum.dal.entity.Event;
import ru.practicum.dal.repository.CategoryRepository;
import ru.practicum.dal.repository.EventRepository;
import ru.practicum.dal.specifications.EventSpecifications;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.dto.event.*;
import ru.practicum.interaction.client.RequestClient;
import ru.practicum.interaction.dto.event.EventFullDto;
import ru.practicum.interaction.dto.event.EventShortDto;
import ru.practicum.interaction.dto.event.EventState;
import ru.practicum.interaction.dto.participation.ConfirmingParticipationRequest;
import ru.practicum.interaction.dto.participation.EventRequestStatusUpdateRequest;
import ru.practicum.interaction.dto.participation.EventRequestStatusUpdateResult;
import ru.practicum.interaction.dto.participation.ParticipationRequestDto;
import ru.practicum.interaction.exception.ConflictException;
import ru.practicum.interaction.exception.EntityNotFoundException;
import ru.practicum.interaction.exception.NotFoundException;
import ru.practicum.interaction.logging.Loggable;
import ru.practicum.interaction.params.EventAdminSearchParam;
import ru.practicum.interaction.params.EventUserSearchParam;
import ru.practicum.interaction.params.PublicEventSearchParam;
import ru.practicum.interaction.params.SortSearchParam;
import ru.practicum.mapper.EventMapper;
import ru.practicum.service.EventService;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;
import static ru.practicum.dal.specifications.EventSpecifications.eventAdminSearchParamSpec;
import static ru.practicum.dal.specifications.EventSpecifications.eventPublicSearchParamSpec;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final EventMapper eventMapper;
    private final StatsClient statsClient;
    private final RequestClient requestClient;

    /**
     * Поиск событий
     */
    @Override
    @Loggable
    public List<EventFullDto> getEventsByParams(EventAdminSearchParam params) {
        Page<Event> events = eventRepository.findAll(eventAdminSearchParamSpec(params), params.getPageable());
        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .toList();

        Map<Long, Long> views = getViews(eventIds);
        Map<Long, Long> confirmedRequests = requestClient.getConfirmedRequestsCount(eventIds);

        return events.get()
                .map(event -> eventMapper.toFullDto(event,
                        views.getOrDefault(event.getId(), 0L),
                        confirmedRequests.getOrDefault(event.getId(), 0L)))
                .toList();
    }

    /**
     * Редактирование данных события и его статуса (отклонение / публикация) администратором.
     */
    @Override
    @Loggable
    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest request) {
        Event event = getEventByIdOrElseThrow(eventId);
        if (event.getState() != EventState.PENDING && request.getStateAction() == AdminEventAction.PUBLISH_EVENT) {
            throw new ConflictException("The event to be published must be in state PENDING, but it " + event.getState());
        }
        if (event.getState() == EventState.PUBLISHED && request.getStateAction() == AdminEventAction.REJECT_EVENT) {
            throw new ConflictException("Cannot reject the event with state PUBLISHED");
        }
        if (event.getEventDate().minusHours(1).isBefore(LocalDateTime.now())) {
            throw new ConflictException("To late to change event");
        }

        Category category = event.getCategory();
        if (request.getCategory() != null) {
            category = getCategoryByIdOrElseThrow(request.getCategory());
        }

        eventMapper.updateEntity(event, request, category);
        if (request.getStateAction() == AdminEventAction.PUBLISH_EVENT) {
            event.setState(EventState.PUBLISHED);
            event.setPublishedOn(LocalDateTime.now());
        } else if (request.getStateAction() == AdminEventAction.REJECT_EVENT) {
            event.setState(EventState.CANCELED);
        }

        eventRepository.save(event);
        log.info("Event {} are updated by Admin", eventId);

        Map<Long, Long> views = getViews(List.of(eventId));
        Map<Long, Long> confirmed = requestClient.getConfirmedRequestsCount(List.of(eventId));

        return eventMapper.toFullDto(event,
                views.getOrDefault(eventId, 0L),
                confirmed.getOrDefault(eventId, 0L));
    }

    @Override
    @Loggable
    public EventFullDto getEventById(Long id) {
        Event event = eventRepository.findByIdAndState(id, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Событие не найдено или не опубликовано"));

        Map<Long, Long> views = getViews(List.of(event.getId()));
        Map<Long, Long> confirmed = requestClient.getConfirmedRequestsCount(List.of(id));
        
        return eventMapper.toFullDto(event,
                views.getOrDefault(id, 0L),
                confirmed.getOrDefault(id, 0L));
    }

    @Override
    @Loggable
    public List<EventShortDto> searchEvents(PublicEventSearchParam param) {
        Page<Event> events = eventRepository.findAll(eventPublicSearchParamSpec(param), param.getPageable());
        List<Long> eventIds = events.stream().map(Event::getId).toList();

        Map<Long, Long> views = getViews(eventIds);
        Map<Long, Long> confirmed = requestClient.getConfirmedRequestsCount(eventIds);

        Stream<EventShortDto> eventShortDtoStream = events.stream()
                .map(event -> {
                    if (param.getOnlyAvailable() && confirmed.get(event.getId()) >= event.getParticipantLimit()) {
                        return null;
                    }
                    return eventMapper.toShortDto(event,
                            views.getOrDefault(event.getId(), 0L),
                            confirmed.getOrDefault(event.getId(), 0L));
                })
                .filter(Objects::nonNull);

        if (param.getSort() == SortSearchParam.VIEWS) {
            eventShortDtoStream = eventShortDtoStream.sorted(Comparator.comparingLong(EventShortDto::getViews));
        }
        return eventShortDtoStream.toList();
    }

    /**
     * Получение событий пользователя
     */
    @Override
    @Loggable
    public List<EventShortDto> getUsersEvents(EventUserSearchParam param) {
        Page<Event> events = eventRepository.findByInitiator(param.getUserId(), param.getPageable());
        List<Long> eventIds = events.stream().map(Event::getId).toList();

        Map<Long, Long> views = getViews(eventIds);
        Map<Long, Long> confirmedRequests = requestClient.getConfirmedRequestsCount(eventIds);

        return events.stream()
                .map(event -> eventMapper.toShortDto(event,
                                views.getOrDefault(event.getId(), 0L),
                                confirmedRequests.getOrDefault(event.getId(), 0L)))
                .toList();
    }

    /**
     * Создание нового события
     */
    @Override
    @Transactional
    @Loggable
    public EventFullDto saveEvent(NewEventDto dto, Long userId) {
        Long categoryId = dto.getCategory();
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category", String.valueOf(categoryId)));

        Event event = eventMapper.toEntity(dto, userId, category);
        eventRepository.saveAndFlush(event);

        log.info("Создано новое событие id={}: {} ", event.getId(), event.getAnnotation());
        return eventMapper.toFullDto(event);
    }

    /**
     * Получение полной информации о событии. Доступно только для автора события
     * @param eventId идентификатор события
     * @param userId идентификатор автора события
     */
    @Override
    @Loggable
    public EventFullDto getEventByIdAndUserId(Long eventId, Long userId) {
        Event event = getEventByIdOrElseThrow(eventId);
        checkUserIsEventInitiator(event, userId);

        Map<Long, Long> views = getViews(List.of(event.getId()));
        Map<Long, Long> confirmed = requestClient.getConfirmedRequestsCount(List.of(eventId));

        return eventMapper.toFullDto(event,
                views.getOrDefault(eventId, 0L),
                confirmed.getOrDefault(eventId, 0L));
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

        Category category = eventToUpdate.getCategory();
        if (request.getCategory() != null) {
            category = getCategoryByIdOrElseThrow(request.getCategory());
        }

        eventMapper.updateEntity(eventToUpdate, request, category);
        if (request.getStateAction() == UserEventAction.CANCEL_REVIEW) {
            eventToUpdate.setState(EventState.CANCELED);
        } else if (request.getStateAction() == UserEventAction.SEND_TO_REVIEW) {
            eventToUpdate.setState(EventState.PENDING);
        }

        eventRepository.save(eventToUpdate);
        log.info("Event {} are updated by author", eventId);

        Map<Long, Long> views = getViews(List.of(eventId));
        Map<Long, Long> confirmed = requestClient.getConfirmedRequestsCount(List.of(eventId));

        return eventMapper.toFullDto(eventToUpdate,
                views.getOrDefault(eventId, 0L),
                confirmed.getOrDefault(eventId, 0L));
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
        return requestClient.getRequestForEvent(eventId);
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
        return requestClient.confirmingRequests(confirmingRequest);
    }

    private Event getEventByIdOrElseThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event", eventId.toString()));
    }

    private Category getCategoryByIdOrElseThrow(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category", categoryId.toString()));
    }

    private void checkUserIsEventInitiator(Event event, Long userId) {
        if (!event.getInitiator().equals(userId)) {
            throw new ConflictException(String.format("User %s is not initiator event %s", userId, event.getId()));
        }
    }
}