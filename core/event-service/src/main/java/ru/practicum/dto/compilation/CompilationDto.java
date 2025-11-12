package ru.practicum.dto.compilation;

import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.practicum.interaction.dto.event.EventShortDto;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompilationDto {
    Long id;

    String title;

    Boolean pinned;

    Set<EventShortDto> events;
}
