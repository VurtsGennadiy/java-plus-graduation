package ru.practicum.ewm.stats.analyzer.dal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.ewm.stats.analyzer.dal.entity.Interaction;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface InteractionRepository extends JpaRepository<Interaction, Long> {
    Optional<Interaction> findByUserIdAndEventId(Long userId, Long eventId);

    List<Interaction> findByEventIdIn(Collection<Long> eventIds);
    
    List<Interaction> findByEventIdInAndUserId(Collection<Long> eventIds, Long userId);

    /**
     * Получить список мероприятий, с которыми пользователь как либо взаимодействовал
     * @param userId - id пользователя
     * @return - список id мероприятий, отсортированных по дате взаимодействия
     */
    @Query("""
            select i.eventId from Interaction as i
            where i.userId = ?1
            order by i.timestamp DESC""")
    Set<Long> findEventIdsForUserInteractions(Long userId);
}
