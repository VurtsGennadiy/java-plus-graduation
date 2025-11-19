package ru.practicum.dto.event;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateEventAdminRequest extends UpdateEventRequest {
    AdminEventAction stateAction;
}
