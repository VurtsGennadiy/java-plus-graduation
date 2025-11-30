package ru.practicum.stats;

import com.google.protobuf.Timestamp;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.UserActionProto;

import java.time.Instant;

@Service
@Slf4j
public class CollectorClient {

    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub userActionClient;

    @CircuitBreaker(name = "stats-breaker", fallbackMethod = "saveUserActionFallback")
    public void saveView(long userId, long eventId) {
        saveUserAction(userId, eventId, ActionTypeProto.ACTION_VIEW);
    }

    @CircuitBreaker(name = "stats-breaker", fallbackMethod = "saveUserActionFallback")
    public void saveLike(long userId, long eventId) {
        saveUserAction(userId, eventId, ActionTypeProto.ACTION_LIKE);
    }

    @CircuitBreaker(name = "stats-breaker", fallbackMethod = "saveUserActionFallback")
    public void saveRegister(long userId, long eventId) {
        saveUserAction(userId, eventId, ActionTypeProto.ACTION_REGISTER);
    }

    private void saveUserAction(long userId, long eventId, ActionTypeProto actionType) {
        Instant now = Instant.now();
        UserActionProto request = UserActionProto.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(actionType)
                .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(now.getEpochSecond())
                        .setNanos(now.getNano()))
                .build();
        userActionClient.collectUserAction(request);
    }

    public void saveUserActionFallback(long userId, long eventId, Throwable throwable) {
        log.warn("UserActionController GRPC service not available. User action not saved.", throwable);
    }
}
