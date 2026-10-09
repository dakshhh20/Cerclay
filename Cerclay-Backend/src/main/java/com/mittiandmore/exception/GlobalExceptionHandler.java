package com.mittiandmore.exception;

import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
        MethodArgumentNotValidException exception,
        HttpServletRequest request
    ) {
        Map<String, String> errors = new HashMap<>();

        exception
            .getBindingResult()
            .getFieldErrors()
            .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        ErrorResponse response = new ErrorResponse(
            "VALIDATION_ERROR",
            "Validation failed",
            errors,
            LocalDateTime.now(),
            request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException exception, HttpServletRequest request) {
        ErrorResponse response = new ErrorResponse(
            exception.getCode(),
            exception.getMessage(),
            null,
            LocalDateTime.now(),
            request.getRequestURI()
        );

        return ResponseEntity.status(exception.getStatus()).body(response);
    }

    @ExceptionHandler({ OptimisticLockException.class, OptimisticLockingFailureException.class })
    public ResponseEntity<ErrorResponse> handleOptimisticLockException(
        Exception exception,
        HttpServletRequest request
    ) {
        logger.warn(
            "Optimistic locking conflict while processing request: {} {}",
            request.getMethod(),
            request.getRequestURI()
        );

        ErrorResponse response = new ErrorResponse(
            "STOCK_UPDATE_CONFLICT",
            "The product stock was updated by another request. Please try again.",
            null,
            LocalDateTime.now(),
            request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception exception, HttpServletRequest request) {
        logger.error(
            "Unhandled exception while processing request: {} {}",
            request.getMethod(),
            request.getRequestURI(),
            exception
        );

        ErrorResponse response = new ErrorResponse(
            "INTERNAL_SERVER_ERROR",
            "An unexpected error occurred",
            null,
            LocalDateTime.now(),
            request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
