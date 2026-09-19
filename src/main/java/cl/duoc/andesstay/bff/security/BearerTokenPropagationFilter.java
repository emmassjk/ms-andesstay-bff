package cl.duoc.andesstay.bff.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

/**
 * Reenvia hacia el microservicio de dominio el MISMO access token de Cognito
 * con el que el frontend llamo al BFF.
 *
 * Ahora que ms-andesstay-catalog y ms-andesstay-audit validan el JWT por su
 * cuenta (mismo User Pool y App Client), exigen el header Authorization en
 * cada llamada; si el BFF no lo reenviara, todas responderian 401.
 *
 * Funciona porque el BFF es una app servlet y los clientes usan .block():
 * la suscripcion ocurre en el mismo hilo del request, donde el
 * SecurityContextHolder todavia tiene la autenticacion del usuario. Si algun
 * dia se llama a estos clientes desde otro hilo (@Async, etc.) no habra
 * token que propagar y la peticion saldra sin Authorization.
 */
public class BearerTokenPropagationFilter implements ExchangeFilterFunction {

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            ClientRequest withToken = ClientRequest.from(request)
                    .headers(headers -> headers.setBearerAuth(jwtAuthentication.getToken().getTokenValue()))
                    .build();
            return next.exchange(withToken);
        }

        return next.exchange(request);
    }
}
