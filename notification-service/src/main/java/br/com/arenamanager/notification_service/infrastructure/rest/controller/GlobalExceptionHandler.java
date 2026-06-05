package br.com.arenamanager.notification_service.infrastructure.rest.controller;

import com.mongodb.MongoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

/**
 * Global exception handler for REST controllers.
 *
 * <p>Maps domain and infrastructure exceptions to HTTP status codes and structured
 * JSON error bodies.
 *
 * <p>Valida: Requisitos 7.4, 7.5</p>
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles validation errors such as invalid {@code playerId} format or
     * {@code size} exceeding the maximum allowed value.
     *
     * @param ex the thrown exception
     * @return HTTP 400 with {@code {"error": "<message>"}}
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Handles MongoDB connectivity or query failures.
     *
     * @param ex the thrown exception
     * @return HTTP 500 with a generic error message
     */
    @ExceptionHandler(MongoException.class)
    public ResponseEntity<Map<String, String>> handleMongoException(MongoException ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Erro interno ao consultar notificações"));
    }
}
