package cl.duoc.andesstay.bff.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class BearerTokenPropagationFilterTest {

    private final BearerTokenPropagationFilter filter = new BearerTokenPropagationFilter();

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void reenviaElAccessTokenDelUsuarioAutenticado() {
        Jwt jwt = Jwt.withTokenValue("access-token-de-prueba")
                .header("alg", "none")
                .claim("sub", "usuario-de-prueba")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        ClientRequest enviada = ejecutarFiltro();

        assertThat(enviada.headers().getFirst(HttpHeaders.AUTHORIZATION))
                .isEqualTo("Bearer access-token-de-prueba");
    }

    @Test
    void sinUsuarioAutenticadoNoAgregaAuthorization() {
        ClientRequest enviada = ejecutarFiltro();

        assertThat(enviada.headers().containsKey(HttpHeaders.AUTHORIZATION)).isFalse();
    }

    private ClientRequest ejecutarFiltro() {
        AtomicReference<ClientRequest> capturada = new AtomicReference<>();
        ExchangeFunction siguiente = request -> {
            capturada.set(request);
            return Mono.just(ClientResponse.create(HttpStatus.OK).build());
        };

        ClientRequest original = ClientRequest.create(HttpMethod.GET, URI.create("http://localhost:8081/api/catalog/units")).build();
        filter.filter(original, siguiente).block();
        return capturada.get();
    }
}
