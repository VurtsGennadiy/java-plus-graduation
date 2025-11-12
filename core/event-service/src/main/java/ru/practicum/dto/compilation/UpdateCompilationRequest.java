package ru.practicum.dto.compilation;

import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateCompilationRequest {
    @Size(min = 1, max = 50)
    String title;

    Boolean pinned;

    Set<Long> events;
}
