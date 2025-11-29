package ru.practicum.ewm.stats.analyzer.dal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.ewm.stats.analyzer.dal.entity.Similarity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SimilarityRepository extends JpaRepository<Similarity, Long> {
    Optional<Similarity> findByEvent1AndEvent2(Long event1, Long event2);

    @Query("""
            select s from Similarity as s
            where s.event1 = ?1 or s.event2 = ?1
            order by s.similarity desc""")
    List<Similarity> findByEvent(Long eventId);


    /**
     * Получить список коэффициентов схожести мероприятий
     * @param eventIds - список id мероприятий, по которым осуществляется поиск
     * @return - список коэффициентов схожести мероприятий, отсортированный по убыванию схожести
     */
    @Query("""
            select s from Similarity as s
            where s.event1 in ?1 or s.event2 in ?1
            order by s.similarity desc""")
    List<Similarity> findByEvent(Collection<Long> eventIds);
}
