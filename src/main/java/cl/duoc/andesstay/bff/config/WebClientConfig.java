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

@Configuration
public class WebClientConfig {

    @Value("${andesstay.services.catalog.base-url}")
    private String catalogBaseUrl;

    @Value("${andesstay.services.audit.base-url}")
    private String auditBaseUrl;

    @Value("${andesstay.services.reservations.base-url}")
    private String reservationsBaseUrl;

    private HttpClient timeoutHttpClient() {
        return HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                .responseTimeout(Duration.ofSeconds(5))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(5, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(5, TimeUnit.SECONDS)));
    }

    @Bean
    public WebClient catalogWebClient() {
        return WebClient.builder()
                .baseUrl(catalogBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(timeoutHttpClient()))
                .filter(new BearerTokenPropagationFilter())
                .build();
    }

    @Bean
    public WebClient auditWebClient() {
        return WebClient.builder()
                .baseUrl(auditBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(timeoutHttpClient()))
                .filter(new BearerTokenPropagationFilter())
                .build();
    }

    @Bean
    public WebClient reservationsWebClient() {
        return WebClient.builder()
                .baseUrl(reservationsBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(timeoutHttpClient()))
                .filter(new BearerTokenPropagationFilter())
                .build();
    }
}