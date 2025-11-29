package ru.practicum.stats;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.proto.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@Slf4j
public class AnalyzerClient {

    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub client;

    @CircuitBreaker(name = "stats-breaker", fallbackMethod = "getSimilarEventsFallback")
    public Map<Long, Double> getSimilarEvents(long eventId, long userId, int maxResults) {
        SimilarEventsRequestProto request = SimilarEventsRequestProto.newBuilder()
                .setEventId(eventId)
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();

        Iterator<RecommendedEventProto> iterator = client.getSimilarEvents(request);
        return asMap(iterator);
    }

    @CircuitBreaker(name = "stats-breaker", fallbackMethod = "getRecommendationsForUserFallback")
    public Map<Long, Double> getRecommendationsForUser(long userId, int maxResults) {
        UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();

        Iterator<RecommendedEventProto> iterator = client.getRecommendationsForUser(request);
        return asMap(iterator);
    }

    @CircuitBreaker(name = "stats-breaker", fallbackMethod = "getInteractionsCountFallback")
    public Map<Long, Double> getInteractionsCount(List<Long> eventIds) {
        InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                .addAllEventId(eventIds)
                .build();

        Iterator<RecommendedEventProto> iterator = client.getInteractionsCount(request);
        return asMap(iterator);
    }

    private Map<Long, Double> asMap(Iterator<RecommendedEventProto> iterator) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
                false
        ).collect(Collectors.toMap(RecommendedEventProto::getEventId, RecommendedEventProto::getScore));
    }

    public Map<Long, Double> getSimilarEventsFallback(long eventId, long userId, int maxResults, Throwable throwable) {
        log.warn("RecommendationsController GRPC service not available. Returned empty similar events", throwable);
        return Collections.emptyMap();
    }

    public Map<Long, Double> getRecommendationsForUserFallback(long userId, int maxResults, Throwable throwable) {
        log.warn("RecommendationsController GRPC service not available. Returned empty recommendations", throwable);
        return Collections.emptyMap();
    }

    public Map<Long, Double> getInteractionsCountFallback(List<Long> eventIds, Throwable throwable) {
        log.warn("RecommendationsController GRPC service not available. Returned empty interactions count", throwable);
        return Collections.emptyMap();
    }
}
