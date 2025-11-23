package ru.practicum.ewm.stats.collector.handler;

import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.collector.kafka.Producer;
import ru.practicum.ewm.stats.proto.UserActionProto;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserActionHandlerImpl implements UserActionHandler {
    private final Producer producer;

    @Override
    public void handle(UserActionProto userAction) {
        log.debug("Handle user action: {}", userAction);
        UserActionAvro userActionAvro = mapToAvro(userAction);
        producer.sendMessage(userActionAvro);
    }

    private UserActionAvro mapToAvro(UserActionProto userActionProto) {
        Timestamp timestamp = userActionProto.getTimestamp();
        return UserActionAvro.newBuilder()
                .setUserId(userActionProto.getUserId())
                .setEventId(userActionProto.getEventId())
                .setActionType(ActionTypeMapper.fromProto(userActionProto.getActionType()))
                .setTimestamp(Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos()))
                .build();
    }
}
