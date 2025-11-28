package ru.practicum.ewm.stats.analyzer.controller;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.stats.analyzer.service.recommendation.RecommendationService;
import ru.practicum.ewm.stats.proto.*;

import java.util.List;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class AnalyzerController extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {
    private final RecommendationService recommendationService;

    /**
     * Получить рекомендованные мероприятия
     */
    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request, StreamObserver<RecommendedEventProto> responseObserver) {
        log.trace("Get recommendations for user: {}", request);
        try {
            List<RecommendedEventProto> recommendations = recommendationService.getRecommendationsForUser(request);
            recommendations.forEach(responseObserver::onNext);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Get recommendations error", e);
            responseObserver.onError(new StatusRuntimeException(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .withCause(e)));
        }
    }

    /**
     * Получить похожие мероприятия
     */
    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request, StreamObserver<RecommendedEventProto> responseObserver) {
        log.trace("Get similar events: {}", request);
        try {
            List<RecommendedEventProto> recommendations = recommendationService.getSimilarEvents(request);
            recommendations.forEach(responseObserver::onNext);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Get similar events error", e);
            responseObserver.onError(new StatusRuntimeException(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .withCause(e)));
        }
    }

    /**
     * Получить рейтинг мероприятий
     */
    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request, StreamObserver<RecommendedEventProto> responseObserver) {
        log.trace("Get interactions count: {}", request);
        try {
            List<RecommendedEventProto> recommendations = recommendationService.getEventsRating(request);
            recommendations.forEach(responseObserver::onNext);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Get interactions count error", e);
            responseObserver.onError(new StatusRuntimeException(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .withCause(e)));
        }
    }
}
