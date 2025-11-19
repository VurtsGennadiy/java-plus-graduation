package ru.practicum.user.dal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    Page<User> findAllByIdIn(List<Long> ids, Pageable pageable);

    Boolean existsByEmail(String name);
}
