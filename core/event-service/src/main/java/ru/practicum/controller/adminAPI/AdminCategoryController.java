package ru.practicum.controller.adminAPI;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.interaction.dto.event.CategoryDto;
import ru.practicum.dto.category.NewCategoryDto;
import ru.practicum.service.CategoryService;

@Validated
@RestController
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {
    private final CategoryService categoryService;
    private final String id = "/{catId}";

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto addCategory(@RequestBody @Valid NewCategoryDto newCategoryDto) {
        return categoryService.addCategory(newCategoryDto);
    }

    @DeleteMapping(id)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable @Positive Long catId) {
        categoryService.deleteCategory(catId);
    }

    @PatchMapping(id)
    public CategoryDto updateCategory(@PathVariable @Positive Long catId,
                                      @RequestBody @Valid NewCategoryDto newCategoryDto) {
        return categoryService.updateCategory(catId, newCategoryDto);
    }
}
