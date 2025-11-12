package ru.practicum.interaction.client.fallback;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.practicum.interaction.client.RequestClient;
import ru.practicum.interaction.dto.participation.ConfirmingParticipationRequest;
import ru.practicum.interaction.dto.participation.EventRequestStatusUpdateResult;
import ru.practicum.interaction.dto.participation.ParticipationRequestDto;
import ru.practicum.interaction.exception.ServiceNotAvailableException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class RequestClientFallbackFactory implements FallbackFactory<RequestClient> {

    @Override
    public RequestClient create(Throwable cause) {
        log.warn("Ошибка обращения к request-service: {}", cause.getMessage());
        if (cause instanceof FeignException.FeignClientException fe) {
            System.out.println(fe.getClass());
            throw fe;
        }

        return new RequestClient() {
            @Override
            public EventRequestStatusUpdateResult confirmingRequests(ConfirmingParticipationRequest request) {
                throw new ServiceNotAvailableException("request-service", cause);
            }

            @Override
            public List<ParticipationRequestDto> getRequestForEvent(Long eventId) {
                return List.of();
            }

            @Override
            public Map<Long, Long> getConfirmedRequestsCount(List<Long> eventIds) {
                return Collections.emptyMap();
            }
        };
    }
}
