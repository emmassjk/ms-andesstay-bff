package cl.duoc.andesstay.bff.controller;

import cl.duoc.andesstay.bff.client.AuditClient;
import cl.duoc.andesstay.bff.dto.AuditEventDto;
import cl.duoc.andesstay.bff.dto.PageResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Puerta de entrada del frontend Angular hacia la trazabilidad de reservas
 * (ms-andesstay-audit). Solo lectura.
 *
 * El rol (ADMIN o AUDITOR) se exige antes de llegar aqui, en SecurityConfig.
 */
@RestController
@RequestMapping("/api/audit")
public class AuditProxyController {

    /** Tope de tamano de pagina para que el frontend no pida listados gigantes. */
    private static final int MAX_PAGE_SIZE = 100;

    private final AuditClient auditClient;

    public AuditProxyController(AuditClient auditClient) {
        this.auditClient = auditClient;
    }

    @GetMapping
    public PageResponse<AuditEventDto> search(
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) String tipoEvento,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return auditClient.search(usuario, tipoEvento, desde, hasta, safePage, safeSize);
    }
}
