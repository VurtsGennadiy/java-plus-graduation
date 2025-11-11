package ru.practicum.interaction.dto.participation;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import ru.practicum.interaction.dto.event.EventFullDto;

/**
 * Запрос на подтверждение или отклонение заявок на участие в событии. Отправляет автор события.
 */
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConfirmingParticipationRequest {
    EventFullDto event;

    EventRequestStatusUpdateRequest updateRequest;
}
