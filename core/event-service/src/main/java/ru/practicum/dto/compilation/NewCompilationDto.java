package ru.practicum.dto.compilation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.LinkedHashSet;
import java.util.Set;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NewCompilationDto {
    @NotBlank
    @Size(min = 1, max = 50)
    String title;

    Boolean pinned = false;

    Set<Long> events = new LinkedHashSet<>();
}
