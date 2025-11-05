package ru.practicum.interaction.exception;

/**
 * Общее исключение. Для ситуаций, когда сущность не найдена в базе по Id.
 */
public class EntityNotFoundException extends RuntimeException {
    private static final String DEFAULT_MESSAGE_TEMPLATE = "%s with id %s not found";

    private final String entityId;
    private final String name;

    public String getEntityId() {
        return entityId;
    }

    public String getName() {
        return name;
    }

    public EntityNotFoundException(String name, String id) {
        super(String.format(DEFAULT_MESSAGE_TEMPLATE, name, id));
        this.entityId = id;
        this.name = name;
    }

    public EntityNotFoundException(String name, String id, String message) {
        super(message);
        this.entityId = id;
        this.name = name;
    }
}
