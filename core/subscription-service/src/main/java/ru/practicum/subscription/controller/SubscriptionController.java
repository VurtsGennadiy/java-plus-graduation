package ru.practicum.subscription.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.interaction.dto.event.EventShortDto;
import ru.practicum.interaction.dto.user.UserShortDto;
import ru.practicum.interaction.exception.BadRequestException;
import ru.practicum.interaction.params.PublicEventSearchParam;
import ru.practicum.interaction.params.SortSearchParam;
import ru.practicum.subscription.service.SubscriptionService;

import java.time.LocalDateTime;
import java.util.List;

@Validated
@RestController
@RequestMapping("/users/{userId}")
@RequiredArgsConstructor
public class SubscriptionController {
    private final SubscriptionService subscriptionService;
    private final String dateTimePattern = "yyyy-MM-dd HH:mm:ss";

    /**
     * Подписаться на пользователя
     * @param userId id пользователя, который подписывается
     * @param followedToUserId id пользователя, на которого подписывается
     */
    @PutMapping("/subscriptions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void createSubscribe(@PathVariable @Positive long userId,
                                @RequestParam @Positive long followedToUserId) {
        subscriptionService.subscribeToUser(userId, followedToUserId);
    }

    /**
     * Отписаться от пользователя
     * @param userId id пользователя, который отписывается
     * @param followedToUserId id пользователя, от которого отписывается
     */
    @DeleteMapping("/subscriptions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSubscribe(@PathVariable @Positive long userId,
                                @RequestParam @Positive long followedToUserId) {
        subscriptionService.unsubscribeFromUser(userId, followedToUserId);
    }

    /**
     * Получить список пользователей, на которых подписан
     * @param userId id пользователя
     * @param from количество элементов, которые нужно пропустить для формирования текущего набора
     * @param size количество элементов в наборе
     */
    @GetMapping("/subscriptions")
    public List<UserShortDto> getSubscribes(@PathVariable @Positive long userId,
                                            @RequestParam(defaultValue = "0") Integer from,
                                            @RequestParam(defaultValue = "10") Integer size) {
        return subscriptionService.getSubscriptions(userId, from, size);
    }

    /**
     * Получить ленту событий от подписок с возможностью фильтрации
     */
    @GetMapping("/events/feed")
    public List<EventShortDto> getEventsFeed(@PathVariable @Positive long userId,
                                             @RequestParam(required = false) String text,
                                             @RequestParam(required = false) List<Long> categories,
                                             @RequestParam(required = false) Boolean paid,
                                             @RequestParam(required = false) @DateTimeFormat(pattern = dateTimePattern) LocalDateTime rangeStart,
                                             @RequestParam(required = false) @DateTimeFormat(pattern = dateTimePattern) LocalDateTime rangeEnd,
                                             @RequestParam(defaultValue = "EVENT_DATE") SortSearchParam sort,
                                             @RequestParam(defaultValue = "false") Boolean onlyAvailable,
                                             @RequestParam(defaultValue = "0") Integer from,
                                             @RequestParam(defaultValue = "10") Integer size) {

        if (rangeStart == null) {
            rangeStart = LocalDateTime.now();
        }

        if (rangeEnd != null && rangeEnd.isBefore(rangeStart)) {
            throw new BadRequestException("rangeEnd can't before rangeStart");
        }

        PublicEventSearchParam param = PublicEventSearchParam.builder()
                .text(text)
                .categories(categories)
                .paid(paid)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .onlyAvailable(onlyAvailable)
                .sort(sort)
                .from(from)
                .size(size)
                .build();

        return subscriptionService.getEventsFeed(userId, param);
    }
}
