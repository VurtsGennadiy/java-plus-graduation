package ru.practicum.subscription.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.interaction.client.EventClient;
import ru.practicum.interaction.client.UserClient;
import ru.practicum.interaction.dto.event.EventShortDto;
import ru.practicum.interaction.dto.user.UserDto;
import ru.practicum.interaction.dto.user.UserShortDto;
import ru.practicum.interaction.exception.EntityNotFoundException;
import ru.practicum.interaction.logging.Loggable;
import ru.practicum.interaction.params.PublicEventSearchParam;
import ru.practicum.subscription.dal.Subscription;
import ru.practicum.subscription.dal.SubscriptionRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionServiceImpl implements SubscriptionService {
    private final SubscriptionRepository subscriptionRepository;
    private final UserClient userClient;
    private final EventClient eventClient;

    @Override
    @Transactional
    @Loggable
    public void subscribeToUser(long followerUserId, long followedToUserId) {
        Subscription subscribe = new Subscription(followerUserId, followedToUserId);
        subscriptionRepository.save(subscribe);
        List<UserDto> user = userClient.getUsers(List.of(followedToUserId), 0, 1);
        if (user.isEmpty()) {
            throw new EntityNotFoundException("User", String.valueOf(followedToUserId));
        }
        log.info("Создана подписка пользователя id = {} на пользователя id = {}", followerUserId, followedToUserId);
    }

    @Override
    @Transactional
    @Loggable
    public void unsubscribeFromUser(long followerUserId, long followedToUserId) {
        subscriptionRepository.deleteByFollowerAndFollowedTo(followerUserId, followedToUserId);
        log.info("Удалена подписка пользователя id = {} на пользователя id = {}", followerUserId, followedToUserId);
    }

    @Override
    @Loggable
    public List<UserShortDto> getSubscriptions(long userId, int from, int size) {
        List<Long> followedUsersIds = subscriptionRepository.findFollowedUsersIds(userId);
        List<UserDto> users = userClient.getUsers(followedUsersIds, from, size);
        return users.stream().map(user -> new UserShortDto(user.getId(), user.getName())).toList();
    }

    @Override
    @Loggable
    public List<EventShortDto> getEventsFeed(long userId, PublicEventSearchParam param) {
        List<Long> followedUsersIds = subscriptionRepository.findFollowedUsersIds(userId);
        param.setUsers(followedUsersIds);

        return eventClient.getEvents(
                param.getText(),
                param.getCategories(),
                param.getUsers(),
                param.getPaid(),
                param.getRangeStart(),
                param.getRangeEnd(),
                param.getSort(),
                param.getOnlyAvailable(),
                param.getFrom(),
                param.getSize()
        );
    }
}
