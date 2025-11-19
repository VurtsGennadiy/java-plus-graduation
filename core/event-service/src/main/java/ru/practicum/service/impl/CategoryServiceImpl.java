package ru.practicum.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.category.NewCategoryDto;
import ru.practicum.dal.entity.Category;
import ru.practicum.interaction.dto.event.CategoryDto;
import ru.practicum.interaction.exception.ConflictException;
import ru.practicum.interaction.exception.EntityNotFoundException;
import ru.practicum.interaction.logging.Loggable;
import ru.practicum.mapper.CategoryMapper;
import ru.practicum.dal.repository.CategoryRepository;
import ru.practicum.service.CategoryService;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    @Loggable
    public CategoryDto addCategory(NewCategoryDto newCategoryDto) {
        checkCategoryNotExists(newCategoryDto.getName());
        Category category = categoryMapper.toEntity(newCategoryDto);
        categoryRepository.save(category);
        CategoryDto dto = categoryMapper.toDto(category);
        log.info("Сохранена новая категория {}", dto);
        return dto;
    }

    @Override
    @Transactional
    @Loggable
    public void deleteCategory(Long catId) {
        categoryRepository.deleteById(catId);
        log.info("Удалена категория id = {}", catId);
    }

    @Override
    @Transactional
    @Loggable
    public CategoryDto updateCategory(Long catId, NewCategoryDto newCategoryDto) {
        Category category = getCategoryByIdOrElseThrow(catId);
        if (category.getName().equals(newCategoryDto.getName())) {
            return categoryMapper.toDto(category);
        }
        checkCategoryNotExists(newCategoryDto.getName());
        category.setName(newCategoryDto.getName());
        categoryRepository.save(category);
        CategoryDto dto = categoryMapper.toDto(category);
        log.info("Обновлена категория {}", dto);
        return dto;
    }

    @Override
    @Loggable
    public List<CategoryDto> getCategories(Integer from, Integer size) {
        return categoryRepository.findAll(PageRequest.of(from / size, size)).stream()
                .map(categoryMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Loggable
    public CategoryDto getCategory(Long catId) {
        Category category = getCategoryByIdOrElseThrow(catId);
        return categoryMapper.toDto(category);
    }

    private Category getCategoryByIdOrElseThrow(Long catId) {
        return categoryRepository.findById(catId)
                .orElseThrow(() -> new EntityNotFoundException("Category", catId.toString()));
    }

    private void checkCategoryNotExists(String categoryName) {
        if (categoryRepository.existsByName(categoryName)) {
            throw new ConflictException(String.format("Категория %s уже существует", categoryName));
        }
    }
}
