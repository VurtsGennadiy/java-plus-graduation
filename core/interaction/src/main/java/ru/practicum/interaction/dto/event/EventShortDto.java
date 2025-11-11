package ru.practicum.interaction.dto.event;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventShortDto {
    String annotation;

    CategoryDto category;

    Long confirmedRequests;

    LocalDateTime eventDate;

    Long id;

    Long initiator;

    Boolean paid;

    String title;

    Long views;
}
