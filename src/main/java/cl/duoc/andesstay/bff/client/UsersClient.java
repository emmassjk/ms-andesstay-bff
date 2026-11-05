package cl.duoc.andesstay.bff.client;

import cl.duoc.andesstay.bff.dto.AuthResponseDto;
import cl.duoc.andesstay.bff.dto.UserLoginRequestDto;
import cl.duoc.andesstay.bff.dto.UserRegisterRequestDto;
import cl.duoc.andesstay.bff.dto.UserResponseDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;

/**
 * Encapsula las llamadas HTTP hacia ms-andesstay-users.
 *
 * <ul>
 *   <li>Registro y login son operaciones publicas (no requieren token).</li>
 *   <li>Las demas operaciones (perfil, listado) requieren el access token de
 *       Cognito del usuario, que se reenvia automaticamente gracias a
 *       {@link cl.duoc.andesstay.bff.security.BearerTokenPropagationFilter}
 *       configurado en el WebClient.</li>
 * </ul>
 *
 * Se usa {@code .block()} porque el BFF es una app servlet (MVC) clasica:
 * cada request ya corre en su propio hilo, asi que no se busca no-bloqueo
 * aqui, solo reusar WebClient como cliente HTTP moderno.
 */
@Component
public class UsersClient {

    private final WebClient usersWebClient;

    public UsersClient(@Qualifier("usersWebClient") WebClient usersWebClient) {
        this.usersWebClient = usersWebClient;
    }

    /**
     * Registra un nuevo usuario. No requiere token (endpoint publico).
     */
    public UserResponseDto register(UserRegisterRequestDto request) {
        return usersWebClient.post()
                .uri("/api/users/register")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(UserResponseDto.class)
                .block(Duration.ofSeconds(6));
    }

    /**
     * Inicia sesion contra Cognito (flujo USER_PASSWORD_AUTH). No requiere
     * token (endpoint publico). Devuelve los tokens OIDC.
     */
    public AuthResponseDto login(UserLoginRequestDto request) {
        return usersWebClient.post()
                .uri("/api/users/login")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AuthResponseDto.class)
                .block(Duration.ofSeconds(6));
    }

    /**
     * Obtiene el perfil del usuario autenticado (token propagado).
     */
    public UserResponseDto getMyProfile() {
        return usersWebClient.get()
                .uri("/api/users/me")
                .retrieve()
                .bodyToMono(UserResponseDto.class)
                .block(Duration.ofSeconds(6));
    }

    /**
     * Obtiene un usuario por su ID (requiere rol ADMIN o RECEPCIONISTA).
     */
    public UserResponseDto findById(String id) {
        return usersWebClient.get()
                .uri("/api/users/{id}", id)
                .retrieve()
                .bodyToMono(UserResponseDto.class)
                .block(Duration.ofSeconds(6));
    }

    /**
     * Lista todos los usuarios (requiere rol ADMIN o RECEPCIONISTA).
     */
    public List<UserResponseDto> findAll() {
        return usersWebClient.get()
                .uri("/api/users")
                .retrieve()
                .bodyToFlux(UserResponseDto.class)
                .collectList()
                .block(Duration.ofSeconds(6));
    }

    /**
     * Actualiza el perfil de un usuario (el propio o por un ADMIN).
     */
    public UserResponseDto update(String id, UserRegisterRequestDto request) {
        return usersWebClient.put()
                .uri("/api/users/{id}", id)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(UserResponseDto.class)
                .block(Duration.ofSeconds(6));
    }
}

