package ru.practicum.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Запрос на создание нового пользователя
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NewUserRequest {

    @NotBlank
    @Size(min = 2, max = 250)
    String name;

    @NotBlank
    @Size(min = 6, max = 254)
    String email;
}
