
package com.banking.platform.common;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Global exception handler for the Banking Services Platform REST APIs.
 *
 * <p>This class provides centralized exception handling across all REST
 * controllers, ensuring consistent HTTP error responses for transaction
 * aggregation, fraud detection, and secure statement delivery services.</p>
 *
 * <p>Each error response contains the following information:</p>
 * <ul>
 *   <li><b>timestamp</b> - UTC timestamp indicating when the error occurred.</li>
 *   <li><b>status</b> - HTTP status code associated with the error.</li>
 *   <li><b>error</b> - Description of the error.</li>
 * </ul>
 *
 * <p>Supported exception categories include:</p>
 * <ul>
 *   <li>Application-specific HTTP status exceptions.</li>
 *   <li>Invalid request arguments.</li>
 *   <li>Request validation failures.</li>
 *   <li>File upload size limit violations.</li>
 * </ul>
 *
 * @see RestControllerAdvice
 * @see ExceptionHandler
 * @see ResponseStatusException
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * Handles exceptions containing an explicit HTTP response status.
     *
     * <p>This method intercepts {@link ResponseStatusException} instances
     * thrown by REST controllers or service components.</p>
     *
     * <p>The HTTP status code and error reason are extracted from the
     * exception and returned in a standardized JSON response.</p>
     *
     * <p>If no reason is provided, the default message
     * {@code "Request failed"} is returned.</p>
     *
     * @param e the {@link ResponseStatusException} containing the HTTP
     *          status code and optional error reason
     * @return a {@link ResponseEntity} containing the HTTP status code
     *         and a JSON-compatible map with timestamp, status, and error
     */
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Map<String, Object>> handle(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode())
                .body(Map.of(
                        "timestamp", Instant.now().toString(),
                        "status", e.getStatusCode().value(),
                        "error", e.getReason() == null
                                ? "Request failed"
                                : e.getReason()
                ));
    }

    /**
     * Handles invalid client requests and request validation exceptions.
     *
     * <p>This method provides centralized handling for the following
     * exception types:</p>
     *
     * <ul>
     *   <li>{@link IllegalArgumentException} - Invalid method arguments
     *       or unsupported input values.</li>
     *   <li>{@link MethodArgumentNotValidException} - Request body
     *       validation failures.</li>
     *   <li>{@link MaxUploadSizeExceededException} - Uploaded files
     *       exceeding the configured maximum upload size.</li>
     * </ul>
     *
     * <p>All supported exceptions are mapped to HTTP
     * {@code 400 Bad Request} with a generic error message to avoid
     * exposing internal application details.</p>
     *
     * @param e the exception triggered by an invalid client request
     * @return a {@link ResponseEntity} with HTTP 400 and a JSON-compatible
     *         map containing the timestamp, status, and error message
     */
    @ExceptionHandler({
            IllegalArgumentException.class,
            MethodArgumentNotValidException.class,
            MaxUploadSizeExceededException.class
    })
    ResponseEntity<Map<String, Object>> badRequest(Exception e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "timestamp", Instant.now().toString(),
                        "status", 400,
                        "error", "Invalid request"
                ));
    }
}
