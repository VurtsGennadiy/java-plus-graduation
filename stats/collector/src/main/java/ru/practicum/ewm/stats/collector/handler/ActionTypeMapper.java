package ru.practicum.ewm.stats.collector.handler;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.proto.ActionTypeProto;

@UtilityClass
public class ActionTypeMapper {
    public static ActionTypeAvro fromProto(ActionTypeProto actionTypeProto) {
        return switch (actionTypeProto) {
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            default -> throw new IllegalArgumentException("Unrecognized Action Type");
        };
    }
}
