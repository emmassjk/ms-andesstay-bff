package cl.duoc.andesstay.bff.config;

import cl.duoc.andesstay.bff.security.BearerTokenPropagationFilter;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Crea los {@link WebClient} que los clientes de cada microservicio de
 * dominio usan para reenviar las peticiones del frontend.
 *
 * Todos comparten la misma configuracion de timeouts y propagan el access
 * token de Cognito del usuario autenticado gracias a
 * {@link BearerTokenPropagationFilter}.
 */
@Configuration
public class WebClientConfig {

    @Value("${andesstay.services.catalog.base-url}")
    private String catalogBaseUrl;

    @Value("${andesstay.services.audit.base-url}")
    private String auditBaseUrl;

    @Value("${andesstay.services.reservations.base-url}")
    private String reservationsBaseUrl;

    @Value("${andesstay.services.users.base-url}")
    private String usersBaseUrl;

    /**
     * Crea un {@link HttpClient} con timeouts estandar.
     */
    private HttpClient timeoutHttpClient() {
        return HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                .responseTimeout(Duration.ofSeconds(5))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(5, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(5, TimeUnit.SECONDS)));
    }

    /**
     * Metodo auxiliar que crea un WebClient con la misma configuracion
     * de timeouts y propagacion de token para cualquier microservicio.
     * Evita repetir el patron builder-connector-filter en cada bean.
     */
    private WebClient buildClient(String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(timeoutHttpClient()))
                .filter(new BearerTokenPropagationFilter())
                .build();
    }

    @Bean
    public WebClient catalogWebClient() {
        return buildClient(catalogBaseUrl);
    }

    @Bean
    public WebClient auditWebClient() {
        return buildClient(auditBaseUrl);
    }

    @Bean
    public WebClient reservationsWebClient() {
        return buildClient(reservationsBaseUrl);
    }

    @Bean
    public WebClient usersWebClient() {
        return buildClient(usersBaseUrl);
    }
}
