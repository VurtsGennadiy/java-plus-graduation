package ru.practicum.dal.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.practicum.dal.entity.Event;
import ru.practicum.interaction.dto.event.EventState;

import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    //@EntityGraph(attributePaths = {"initiator", "category"})
    Page<Event> findByInitiator(Long initiator, Pageable pageable);

    //@EntityGraph(attributePaths = {"initiator", "category"})
    Optional<Event> findByIdAndState(Long id, EventState state);

    //@EntityGraph(attributePaths = {"initiator", "category"})
    Page<Event> findAll(Specification<Event> specification, Pageable pageable);
}
