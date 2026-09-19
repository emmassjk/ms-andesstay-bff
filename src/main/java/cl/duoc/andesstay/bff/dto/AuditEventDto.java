package cl.duoc.andesstay.bff.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

/**
 * Evento de auditoria tal como lo devuelve ms-andesstay-audit (GET /api/audit).
 * DTO propio del BFF para no acoplar el contrato publico a los detalles
 * internos del microservicio.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AuditEventDto(
        Long id,
        String eventType,
        String aggregateId,
        String actor,
        Instant occurredAt,
        Instant receivedAt,
        String traceId,
        String correlationId
) {
}
