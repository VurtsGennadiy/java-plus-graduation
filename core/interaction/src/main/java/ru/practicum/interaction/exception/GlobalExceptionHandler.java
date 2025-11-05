package ru.practicum.interaction.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import ru.practicum.interaction.dto.ApiError;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler
    public ResponseEntity<ApiError> handleNotFoundException(NotFoundException e) {
        log.info("404 {}", e.getMessage());
        String notFoundReason = "The required object was not found";
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.builder()
                .status(HttpStatus.NOT_FOUND.toString())
                .reason(notFoundReason)
                .message(e.getMessage())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @ExceptionHandler
    public ResponseEntity<ApiError> handleEntityNotFoundException(EntityNotFoundException ex) {
        HttpStatus responseStatus = HttpStatus.NOT_FOUND;
        ApiError errorResponse = ApiError.builder()
                .status(responseStatus.toString())
                .reason("The required object was not found")
                .message(ex.getMessage())
                .build();
        log.warn(ex.getMessage());
        return new ResponseEntity<>(errorResponse, responseStatus);
    }

    /**
     * Нарушение ограничений базы данных
     */
    @ExceptionHandler({DataIntegrityViolationException.class, ConflictException.class})
    public ResponseEntity<ApiError> handleConflictException(Exception e) {
        HttpStatus responseStatus = HttpStatus.CONFLICT;
        log.warn("Integrity constraint violate: {}", e.getMessage());
        String conflictReason = "Integrity constraint has been violated";
        return ResponseEntity.status(responseStatus).body(ApiError.builder()
                .status(responseStatus.toString())
                .reason(conflictReason)
                .message(e.getMessage())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, BadRequestException.class})
    public ResponseEntity<ApiError> handleBadRequestException(Exception e) {
        log.warn("Invalid request: {}", e.getMessage());
        String badRequestReason = "Incorrectly made request";
        return ResponseEntity.badRequest()
                .body(ApiError.builder()
                        .status(HttpStatus.BAD_REQUEST.toString())
                        .reason(badRequestReason)
                        .message(e.getMessage())
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    /**
     * Нарушение валидации @Validation
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolationException(ConstraintViolationException exception) {
        HttpStatus responseStatus = HttpStatus.BAD_REQUEST;
        String message = exception.getConstraintViolations().stream()
                .map(violation -> {
                    String path = violation.getPropertyPath().toString();
                    String fieldName = path.substring(path.lastIndexOf(".") + 1);
                    return String.format("Поле %s %s. ", fieldName, violation.getMessage());
                })
                .collect(Collectors.joining(" "))
                .strip();

        ApiError errorResponse = ApiError.builder()
                .status(responseStatus.toString())
                .reason("Incorrectly made request")
                .message(message)
                .build();

        log.warn("Invalid request: {}", message);
        return new ResponseEntity<>(errorResponse, responseStatus);
    }

    /**
     * Нарушение валидации @Valid
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        HttpStatus responseStatus = HttpStatus.BAD_REQUEST;

        StringBuilder userMessageBuilder = new StringBuilder();
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error -> {
            String field = error.getField();
            String message = error.getDefaultMessage();
            errors.put(field, message);
        });

        errors.forEach((field, message) -> {
            userMessageBuilder.append("Поле ");
            userMessageBuilder.append(field);
            userMessageBuilder.append(" ");
            userMessageBuilder.append(message);
            userMessageBuilder.append(". ");
        });

        ApiError errorResponse = ApiError.builder()
                .status(responseStatus.toString())
                .message(userMessageBuilder.toString().strip())
                .reason("Incorrectly made request")
                .build();

        log.warn("Invalid request: {}", e.getMessage());
        return new ResponseEntity<>(errorResponse, responseStatus);
    }

    @ExceptionHandler
    public ResponseEntity<ApiError> handleException(Exception ex) {
        HttpStatus responseStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        ApiError errorResponse = ApiError.builder()
                .status(responseStatus.toString())
                .reason("Internal server error")
                .message(ex.getMessage())
                .build();
        log.error("Error:", ex);
        return new ResponseEntity<>(errorResponse, responseStatus);
    }
}
