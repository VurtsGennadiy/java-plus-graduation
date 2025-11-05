package ru.practicum.interaction.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Around("@annotation(ru.practicum.interaction.logging.Loggable)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        log.debug("Entering method: {}", joinPoint.getSignature());

        // Log request parameters
        Object[] args = joinPoint.getArgs();
        log.debug("Request Parameters: {}", args);

        Object result = joinPoint.proceed(); // Proceed with method execution

        // Log response
        log.debug("Exiting method: {} - Response: {}", joinPoint.getSignature(), result);

        return result;
    }
}

