package com.ouroboros.pestadiumbookingbe.util;

import com.ouroboros.pestadiumbookingbe.service.NotificationService;
import io.swagger.v3.oas.annotations.Hidden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Hidden
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<String> handleDatabaseExceptions(DataAccessException ex) {
        // Log the error for debugging
        logger.error("Database access error occurred: {}", ex.getMessage(), ex);
        // Return a user-friendly error message
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("The service is temporarily unavailable due to database issues. Please try again later.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGenericException(Exception ex) {
        logger.error("Unhandled exception: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An unexpected error occurred. Please try again later.");
    }
}
