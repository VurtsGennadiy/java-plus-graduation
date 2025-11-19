package ru.practicum.dal.repository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.dal.entity.Compilation;

import java.util.List;

public interface CompilationRepository extends JpaRepository<Compilation, Long> {

    List<Compilation> findByPinned(boolean pinned, PageRequest pageRequest);
}
