package cl.duoc.andesstay.bff.config;

import cl.duoc.andesstay.bff.security.BearerTokenPropagationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Clientes HTTP usados por el BFF para reenviar (proxy) las peticiones ya
 * autenticadas/autorizadas hacia los microservicios de dominio.
 *
 * Ambos clientes reenvian el access token de Cognito del usuario
 * (BearerTokenPropagationFilter), porque catalog y audit validan el JWT por
 * su cuenta y responden 401 si no llega el header Authorization.
 */
@Configuration
public class WebClientConfig {

    @Value("${andesstay.services.catalog.base-url}")
    private String catalogBaseUrl;

    @Value("${andesstay.services.audit.base-url}")
    private String auditBaseUrl;

    /** Cliente hacia ms-andesstay-catalog. */
    @Bean
    public WebClient catalogWebClient() {
        return WebClient.builder()
                .baseUrl(catalogBaseUrl)
                .filter(new BearerTokenPropagationFilter())
                .build();
    }

    /** Cliente hacia ms-andesstay-audit. */
    @Bean
    public WebClient auditWebClient() {
        return WebClient.builder()
                .baseUrl(auditBaseUrl)
                .filter(new BearerTokenPropagationFilter())
                .build();
    }
}
