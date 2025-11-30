package ru.practicum.event.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.event.dal.entity.Category;
import ru.practicum.event.dal.entity.Event;
import ru.practicum.event.dal.repository.CategoryRepository;
import ru.practicum.event.dal.repository.EventRepository;
import ru.practicum.event.dto.event.*;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.service.EventService;
import ru.practicum.interaction.client.RequestClient;
import ru.practicum.interaction.dto.event.EventFullDto;
import ru.practicum.interaction.dto.event.EventShortDto;
import ru.practicum.interaction.dto.event.EventState;
import ru.practicum.interaction.dto.participation.*;
import ru.practicum.interaction.exception.BadRequestException;
import ru.practicum.interaction.exception.ConflictException;
import ru.practicum.interaction.exception.EntityNotFoundException;
import ru.practicum.interaction.exception.NotFoundException;
import ru.practicum.interaction.logging.Loggable;
import ru.practicum.interaction.params.EventAdminSearchParam;
import ru.practicum.interaction.params.EventUserSearchParam;
import ru.practicum.interaction.params.PublicEventSearchParam;
import ru.practicum.interaction.params.SortSearchParam;
import ru.practicum.stats.AnalyzerClient;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

import static ru.practicum.event.dal.specifications.EventSpecifications.eventAdminSearchParamSpec;
import static ru.practicum.event.dal.specifications.EventSpecifications.eventPublicSearchParamSpec;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final EventMapper eventMapper;
    private final RequestClient requestClient;
    private final AnalyzerClient analyzerClient;

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

        Map<Long, Double> ratings = analyzerClient.getInteractionsCount(eventIds);
        Map<Long, Long> confirmedRequests = requestClient.getConfirmedRequestsCount(eventIds);

        return events.get()
                .map(event -> eventMapper.toFullDto(event,
                        ratings.getOrDefault(event.getId(), 0d),
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

        Map<Long, Double> ratings = analyzerClient.getInteractionsCount(List.of(eventId));
        Map<Long, Long> confirmed = requestClient.getConfirmedRequestsCount(List.of(eventId));

        return eventMapper.toFullDto(event,
                ratings.getOrDefault(eventId, 0d),
                confirmed.getOrDefault(eventId, 0L));
    }

    @Override
    @Loggable
    public EventFullDto getEventById(Long id) {
        Event event = eventRepository.findByIdAndState(id, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Событие не найдено или не опубликовано"));

        Map<Long, Double> ratings = analyzerClient.getInteractionsCount(List.of(id));
        Map<Long, Long> confirmed = requestClient.getConfirmedRequestsCount(List.of(id));
        
        return eventMapper.toFullDto(event,
                ratings.getOrDefault(id, 0d),
                confirmed.getOrDefault(id, 0L));
    }

    @Override
    @Loggable
    public List<EventShortDto> searchEvents(PublicEventSearchParam param) {
        Page<Event> events = eventRepository.findAll(eventPublicSearchParamSpec(param), param.getPageable());
        List<Long> eventIds = events.stream().map(Event::getId).toList();

        Map<Long, Double> ratings = analyzerClient.getInteractionsCount(eventIds);
        Map<Long, Long> confirmed = requestClient.getConfirmedRequestsCount(eventIds);

        Stream<EventShortDto> eventShortDtoStream = events.stream()
                .map(event -> {
                    if (param.getOnlyAvailable() && confirmed.get(event.getId()) >= event.getParticipantLimit()) {
                        return null;
                    }
                    return eventMapper.toShortDto(event,
                            ratings.getOrDefault(event.getId(), 0d),
                            confirmed.getOrDefault(event.getId(), 0L));
                })
                .filter(Objects::nonNull);

        if (param.getSort() == SortSearchParam.RATING) {
            eventShortDtoStream = eventShortDtoStream.sorted(Comparator.comparingDouble(EventShortDto::getRating));
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

        Map<Long, Double> ratings = analyzerClient.getInteractionsCount(eventIds);
        Map<Long, Long> confirmedRequests = requestClient.getConfirmedRequestsCount(eventIds);

        return events.stream()
                .map(event -> eventMapper.toShortDto(event,
                                ratings.getOrDefault(event.getId(), 0d),
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

        Map<Long, Double> ratings = analyzerClient.getInteractionsCount(List.of(event.getId()));
        Map<Long, Long> confirmed = requestClient.getConfirmedRequestsCount(List.of(eventId));

        return eventMapper.toFullDto(event,
                ratings.getOrDefault(eventId, 0d),
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

        Map<Long, Double> ratings = analyzerClient.getInteractionsCount(List.of(eventId));
        Map<Long, Long> confirmed = requestClient.getConfirmedRequestsCount(List.of(eventId));

        return eventMapper.toFullDto(eventToUpdate,
                ratings.getOrDefault(eventId, 0d),
                confirmed.getOrDefault(eventId, 0L));
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

    /**
     * Получить рекомендованные мероприятия для пользователя
     */
    @Loggable
    @Override
    public List<EventShortDto> getRecommendedEvents(Long userId, Integer maxResults) {
        Map<Long, Double> recommended = analyzerClient.getRecommendationsForUser(userId, 100);
        if (recommended.isEmpty()) {
            return List.of();
        }
        List<Long> eventIds = new ArrayList<>(recommended.keySet());

        List<Event> events = eventRepository.findAllById(eventIds);
        Map<Long, Double> ratings = analyzerClient.getInteractionsCount(eventIds);
        Map<Long, Long> confirmedRequests = requestClient.getConfirmedRequestsCount(eventIds);

        return events.stream()
                .map(event -> eventMapper.toShortDto(event,
                        ratings.getOrDefault(event.getId(), 0d),
                        confirmedRequests.getOrDefault(event.getId(), 0L)))
                .toList();
    }

    /**
     * Получить похожие мероприятия
     * @param eventId - id мероприятия для которого нужно найти похожие
     * @param userId - id пользователя, для которого нужно исключить просмотренные мероприятий
     * @param maxResults - ограничение на количество
     * @return - список похожих событий List<EventShortDto>
     */
    @Loggable
    @Override
    public List<EventShortDto> getSimilarEvents(Long eventId, Long userId, Integer maxResults) {
        Map<Long, Double> similar = analyzerClient.getSimilarEvents(eventId, userId, maxResults);
        if (similar.isEmpty()) {
            return List.of();
        }
        List<Long> eventIds = new ArrayList<>(similar.keySet());

        List<Event> events = eventRepository.findAllById(eventIds);
        Map<Long, Double> ratings = analyzerClient.getInteractionsCount(eventIds);
        Map<Long, Long> confirmedRequests = requestClient.getConfirmedRequestsCount(eventIds);

        return events.stream()
                .map(event -> eventMapper.toShortDto(event,
                        ratings.getOrDefault(event.getId(), 0d),
                        confirmedRequests.getOrDefault(event.getId(), 0L)))
                .toList();
    }

    /**
     * Поставить лайк мероприятию.
     * Можно поставить лайк только посещённому мероприятию.
     */
    @Override
    @Loggable
    public void likeEvent(Long eventId, Long userId) {
        Optional<ParticipationRequestDto> request = requestClient.getRequestForEvent(eventId).stream()
                .filter(r -> r.getRequester().equals(userId))
                .findFirst();

        boolean requestIsConfirmed = request.isPresent() && RequestStatus.CONFIRMED == request.get().getStatus();

        if (!requestIsConfirmed) {
            throw new BadRequestException(
                    String.format("Event %s cannot be liked by user %s. The user is not a participant in the event.", eventId, userId));
        }
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