package ru.practicum.ewm.stats.collector.handler;

import ru.practicum.ewm.stats.proto.UserActionProto;

public interface UserActionHandler {
    void handle(UserActionProto userAction);
}
