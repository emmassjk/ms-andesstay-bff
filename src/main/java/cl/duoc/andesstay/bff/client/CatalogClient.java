package cl.duoc.andesstay.bff.client;

import cl.duoc.andesstay.bff.dto.UnitDto;
import cl.duoc.andesstay.bff.dto.UnitRequestDto;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * Encapsula las llamadas HTTP hacia ms-andesstay-catalog. El controller solo
 * conoce esta interfaz; si mas adelante el catalogo pasa a exponerse via
 * AWS API Gateway con otra ruta/base-url, solo cambia application.yml.
 *
 * Se usa .block() porque el BFF es una app servlet (MVC) clasica: cada
 * request ya corre en su propio hilo, asi que no se busca no-bloqueo aqui,
 * solo reusar WebClient como cliente HTTP moderno.
 */
@Component
public class CatalogClient {

    private final WebClient catalogWebClient;

    public CatalogClient(WebClient catalogWebClient) {
        this.catalogWebClient = catalogWebClient;
    }

    public List<UnitDto> findAll() {
        return catalogWebClient.get()
                .uri("/api/catalog/units")
                .retrieve()
                .bodyToFlux(UnitDto.class)
                .collectList()
                .block();
    }

    public UnitDto findById(Long id) {
        return catalogWebClient.get()
                .uri("/api/catalog/units/{id}", id)
                .retrieve()
                .bodyToMono(UnitDto.class)
                .block();
    }

    public UnitDto create(UnitRequestDto request) {
        return catalogWebClient.post()
                .uri("/api/catalog/units")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(UnitDto.class)
                .block();
    }

    public UnitDto update(Long id, UnitRequestDto request) {
        return catalogWebClient.put()
                .uri("/api/catalog/units/{id}", id)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(UnitDto.class)
                .block();
    }

    public void delete(Long id) {
        catalogWebClient.delete()
                .uri("/api/catalog/units/{id}", id)
                .retrieve()
                .toBodilessEntity()
                .block();
    }
}
