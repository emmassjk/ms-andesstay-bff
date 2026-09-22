package cl.duoc.andesstay.bff.client;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import cl.duoc.andesstay.bff.dto.UnitDto;
import cl.duoc.andesstay.bff.dto.UnitRequestDto;

/**
 * Encapsula las llamadas HTTP hacia ms-andesstay-catalog. El controller solo
 * conoce esta interfaz; si mas adelante el catalogo pasa a exponerse via
 * AWS API Gateway con otra ruta/base-url, solo cambia application.yml.
 *
 * El access token de Cognito del usuario se reenvia en cada llamada
 * (ver BearerTokenPropagationFilter en WebClientConfig).
 *
 * Se usa .block() porque el BFF es una app servlet (MVC) clasica: cada
 * request ya corre en su propio hilo, asi que no se busca no-bloqueo aqui,
 * solo reusar WebClient como cliente HTTP moderno.
 */
@Component
public class CatalogClient {

    private final WebClient catalogWebClient;

    public CatalogClient(@Qualifier("catalogWebClient") WebClient catalogWebClient) {
        this.catalogWebClient = catalogWebClient;
    }

    public List<UnitDto> findAll() {
        return catalogWebClient.get()
                .uri("/api/catalog/units")
                .retrieve()
                .bodyToFlux(UnitDto.class)
                .collectList()
                .block(Duration.ofSeconds(6));
    }

    public UnitDto findById(Long id) {
        return catalogWebClient.get()
                .uri("/api/catalog/units/{id}", id)
                .retrieve()
                .bodyToMono(UnitDto.class)
                .block(Duration.ofSeconds(6));
    }

    public UnitDto create(UnitRequestDto request) {
        return catalogWebClient.post()
                .uri("/api/catalog/units")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(UnitDto.class)
                .block(Duration.ofSeconds(6));
    }

    public UnitDto update(Long id, UnitRequestDto request) {
        return catalogWebClient.put()
                .uri("/api/catalog/units/{id}", id)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(UnitDto.class)
                .block(Duration.ofSeconds(6));
    }

    public void delete(Long id) {
        catalogWebClient.delete()
                .uri("/api/catalog/units/{id}", id)
                .retrieve()
                .toBodilessEntity()
                .block(Duration.ofSeconds(6));
    }
}
