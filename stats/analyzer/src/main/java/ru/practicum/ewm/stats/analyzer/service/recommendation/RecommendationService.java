package ru.practicum.ewm.stats.analyzer.service.recommendation;

import ru.practicum.ewm.stats.proto.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.proto.UserPredictionsRequestProto;

import java.util.List;

public interface RecommendationService {
    /**
     * Получить рекомендации мероприятий, основанные на предсказании оценки
     */
    List<RecommendedEventProto> getRecommendationsForUser(UserPredictionsRequestProto request);

    /**
     * Получить мероприятия, похожие на заданное
     */
    List<RecommendedEventProto> getSimilarEvents(SimilarEventsRequestProto request);

    /**
     * Получить рейтинг мероприятий
     * Рейтинг мероприятия это сумма весов максимальных взаимодействий пользователя с ним
     */
    List<RecommendedEventProto> getEventsRating(InteractionsCountRequestProto request);
}
