package ru.practicum.participation.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.practicum.participation.dal.RequestStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ParticipationRequestDto {
    LocalDateTime created;

    Long event;

    Long id;

    Long requester;

    RequestStatus status;
}
