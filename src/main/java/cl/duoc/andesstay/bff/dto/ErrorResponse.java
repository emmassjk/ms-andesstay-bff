package cl.duoc.andesstay.bff.dto;

import java.time.Instant;

/**
 * Cuerpo de error homogeneo devuelto por el BFF, tanto para fallas de
 * autenticacion/autorizacion como para errores propagados desde los
 * microservicios de dominio.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now(), status, error, message, path);
    }
}
