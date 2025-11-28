package ru.practicum.ewm.stats.analyzer.service.interaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.analyzer.dal.entity.Interaction;
import ru.practicum.ewm.stats.analyzer.dal.mapper.InteractionMapper;
import ru.practicum.ewm.stats.analyzer.dal.repository.InteractionRepository;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InteractionServiceImpl implements InteractionService {
    private final InteractionRepository repository;

    @Override
    @Transactional
    public void handleInteraction(UserActionAvro avro) {
        log.debug("Обработка события действия пользователя {}", avro);
        Interaction interaction = InteractionMapper.fromAvro(avro);

        Optional<Interaction> optionalOld = repository.findByUserIdAndEventId(interaction.getUserId(), interaction.getEventId());
        if (optionalOld.isEmpty()) {
            saveNewInteraction(interaction);
        } else {
            updateInteraction(interaction, optionalOld.get());
        }
    }

    private void saveNewInteraction(Interaction interaction) {
        repository.save(interaction);
        log.info("Сохранено новое действие пользователя: {}", interaction);
    }

    private void updateInteraction(Interaction newInteraction, Interaction forUpdate) {
        log.debug("Обновление рейтинга взаимодействия пользователя {} для события {}, старое значение {}, полученное значение {}",
                newInteraction.getUserId(), newInteraction.getEventId(), forUpdate.getRating(), newInteraction.getRating());

        if (forUpdate.getRating() >= newInteraction.getRating()) {
            log.debug("Новое действие пользователя {} для события {} имеет рейтинг {} ниже, чем сохранённое {}, обновление не требуется",
                    newInteraction.getUserId(), newInteraction.getEventId(), newInteraction.getRating(), forUpdate.getRating());
            return;
        }

        forUpdate.setRating(newInteraction.getRating());
        repository.save(forUpdate);
        log.info("Обновлен рейтинг взаимодействия пользователя {} для события {}: новое значение {}",
                forUpdate.getUserId(), forUpdate.getEventId(), forUpdate.getRating());
    }
}
