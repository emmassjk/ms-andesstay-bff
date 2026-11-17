package cl.duoc.andesstay.bff.exception;

import cl.duoc.andesstay.bff.dto.ErrorResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.stream.Collectors;

/**
 * Traduce las fallas que puede producir el proxy hacia los microservicios de
 * dominio (o la validacion de entrada) en respuestas JSON consistentes con
 * ErrorResponse, en vez de dejar que se filtren stack traces o el formato
 * de error por defecto de Spring.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * El microservicio de dominio respondio con un error HTTP (4xx/5xx): se propaga el mismo status
     * y se extrae el mensaje de detalle real (ej. motivo del 409 Conflicto) para que el frontend
     * pueda mostrar la causa exacta en vez de un texto generico.
     */
    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ErrorResponse> handleDownstreamHttpError(WebClientResponseException ex,
                                                                    HttpServletRequest request) {
        HttpStatusCode status = ex.getStatusCode();
        String detailMessage = extractMessage(ex.getResponseBodyAsString(), ex.getStatusText());

        ErrorResponse body = ErrorResponse.of(
                status.value(),
                status.toString(),
                detailMessage,
                request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }

    /** No se pudo ni siquiera contactar al microservicio (caido, DNS, timeout, etc.). */
    @ExceptionHandler(WebClientRequestException.class)
    public ResponseEntity<ErrorResponse> handleDownstreamUnavailable(WebClientRequestException ex,
                                                                      HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.BAD_GATEWAY.value(),
                "bad_gateway",
                "No fue posible comunicarse con el microservicio de dominio.",
                request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
    }

    /** Cuerpo de la peticion invalido segun las anotaciones @NotBlank/@Positive de los DTO. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationError(MethodArgumentNotValidException ex,
                                                                 HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining("; "));

        ErrorResponse body = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "bad_request",
                message,
                request.getRequestURI());
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Extrae el mensaje de error legible del cuerpo de respuesta del microservicio downstream.
     * Si el cuerpo es un JSON con campo "message" o "error", se extrae su valor.
     */
    private String extractMessage(String responseBody, String statusText) {
        if (responseBody == null || responseBody.isBlank()) {
            return "El microservicio de dominio respondio con un error: " + statusText;
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            if (root.has("message") && !root.get("message").asText().isBlank()) {
                return root.get("message").asText();
            }
            if (root.has("error") && !root.get("error").asText().isBlank()) {
                return root.get("error").asText();
            }
        } catch (Exception ignored) {
            if (responseBody.length() < 200 && !responseBody.contains("<html>")) {
                return responseBody.trim();
            }
        }
        return "El microservicio de dominio respondio con un error: " + statusText;
    }
}
