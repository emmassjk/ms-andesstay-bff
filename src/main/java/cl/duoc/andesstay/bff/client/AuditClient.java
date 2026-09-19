package cl.duoc.andesstay.bff.client;

import cl.duoc.andesstay.bff.dto.AuditEventDto;
import cl.duoc.andesstay.bff.dto.PageResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.Optional;

/**
 * Encapsula las llamadas HTTP hacia ms-andesstay-audit (solo lectura).
 * El access token de Cognito del usuario se reenvia en cada llamada (ver
 * BearerTokenPropagationFilter); audit solo responde a ADMIN y AUDITOR.
 */
@Component
public class AuditClient {

    private final WebClient auditWebClient;

    public AuditClient(@Qualifier("auditWebClient") WebClient auditWebClient) {
        this.auditWebClient = auditWebClient;
    }

    public PageResponse<AuditEventDto> search(String usuario, String tipoEvento,
                                              Instant desde, Instant hasta,
                                              int page, int size) {
        return auditWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/audit")
                        .queryParamIfPresent("usuario", Optional.ofNullable(usuario))
                        .queryParamIfPresent("tipoEvento", Optional.ofNullable(tipoEvento))
                        .queryParamIfPresent("desde", Optional.ofNullable(desde).map(Instant::toString))
                        .queryParamIfPresent("hasta", Optional.ofNullable(hasta).map(Instant::toString))
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<PageResponse<AuditEventDto>>() { })
                .block();
    }
}
