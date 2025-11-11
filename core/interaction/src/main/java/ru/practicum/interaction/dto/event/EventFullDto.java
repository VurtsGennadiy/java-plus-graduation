package ru.practicum.interaction.dto.event;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventFullDto {
    String annotation;
    CategoryDto category;
    Long confirmedRequests;
    LocalDateTime eventDate;
    Long id;
    Long initiator;
    Boolean paid;
    String title;
    Long views;
    LocalDateTime createdOn;
    String description;
    LocationDto location;
    Integer participantLimit;
    LocalDateTime publishedOn;
    Boolean requestModeration;
    EventState state;
}
