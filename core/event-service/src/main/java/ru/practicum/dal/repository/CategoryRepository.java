package ru.practicum.dal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.dal.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Boolean existsByName(String name);
}
