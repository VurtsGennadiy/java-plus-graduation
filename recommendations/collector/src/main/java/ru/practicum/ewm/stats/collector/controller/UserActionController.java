package ru.practicum.ewm.stats.collector.controller;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.stats.collector.UserActionControllerGrpc;
import ru.practicum.ewm.stats.collector.handler.UserActionHandler;
import ru.practicum.ewm.stats.proto.UserActionProto;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserActionController extends UserActionControllerGrpc.UserActionControllerImplBase {
    private final UserActionHandler handler;

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        log.trace("Collect user action: {}", request);
        try {
            handler.handle(request);

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Handle user action error", e);
            responseObserver.onError(new StatusRuntimeException(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .withCause(e)));
        }
    }
}
