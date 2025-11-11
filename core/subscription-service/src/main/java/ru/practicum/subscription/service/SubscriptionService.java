package ru.practicum.subscription.service;

import ru.practicum.interaction.dto.event.EventShortDto;
import ru.practicum.interaction.dto.user.UserShortDto;
import ru.practicum.interaction.params.PublicEventSearchParam;

import java.util.List;

public interface SubscriptionService {
    /**
     * Подписаться на пользователя
     * @param followerUserId id пользователя, который подписывается
     * @param followedToUserId id пользователя, на которого подписывается
     */
    void subscribeToUser(long followerUserId, long followedToUserId);


    /**
     * Отписаться от пользователя
     * @param followerUserId id пользователя, который отписывается
     * @param followedToUserId id пользователя, от которого отписывается
     */
    void unsubscribeFromUser(long followerUserId, long followedToUserId);

    /**
     * Получить список пользователей, на которых подписан
     * @param userId id пользователя
     * @param from количество элементов, которые нужно пропустить для формирования текущего набора
     * @param size количество элементов в наборе
     */
    List<UserShortDto> getSubscriptions(long userId, int from, int size);

    /**
     * Получить ленту событий от подписок
     * @param userId id пользователя
     * @param param параметры фильтрации
     */
    List<EventShortDto> getEventsFeed(long userId, PublicEventSearchParam param);
}
