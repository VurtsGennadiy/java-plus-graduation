package ru.practicum.interaction.exception;

import lombok.Getter;

/**
 * Исключение, возникающее при недоступности сервиса
 */
public class ServiceNotAvailableException extends RuntimeException {
    private static final String MESSAGE_TEMPLATE = "Service '%s' is not available";

    @Getter
    private final String serviceName;

    public ServiceNotAvailableException(String serviceName, Throwable cause) {
        super(String.format(MESSAGE_TEMPLATE, serviceName), cause);
        this.serviceName = serviceName;
    }

    public ServiceNotAvailableException(String serviceName) {
        super(String.format(MESSAGE_TEMPLATE, serviceName));
        this.serviceName = serviceName;
    }
}
