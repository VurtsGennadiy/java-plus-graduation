package ru.practicum.interaction.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ApiError {

    String message;

    String reason;

    String status;

    @Builder.Default
    LocalDateTime timestamp = LocalDateTime.now();
}
