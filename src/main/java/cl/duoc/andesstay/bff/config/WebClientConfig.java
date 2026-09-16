package cl.duoc.andesstay.bff.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${andesstay.services.catalog.base-url}")
    private String catalogBaseUrl;

    /**
     * Cliente HTTP usado por el BFF para reenviar (proxy) las peticiones ya
     * autenticadas/autorizadas hacia el microservicio ms-andesstay-catalog.
     * El JWT NO se reenvia hacia el microservicio de dominio en esta fase
     * (el catalogo aun no valida JWT, ver su README "Pendiente"); el punto
     * de confianza es el BFF.
     */
    @Bean
    public WebClient catalogWebClient() {
        return WebClient.builder()
                .baseUrl(catalogBaseUrl)
                .build();
    }
}
