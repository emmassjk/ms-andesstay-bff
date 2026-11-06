package cl.duoc.andesstay.bff.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Encapsula las llamadas HTTP hacia ms-andesstay-users.
 *
 * <ul>
 *   <li>Registro (POST /api/users) es publico (no requiere token).</li>
 *   <li>Las demas operaciones (perfil, listado, actualizacion, baja) requieren
 *       el access token de Cognito del usuario, que se reenvia automaticamente
 *       gracias a {@link cl.duoc.andesstay.bff.security.BearerTokenPropagationFilter}
 *       configurado en el WebClient.</li>
 * </ul>
 *
 * Se usa {@code Map<String,Object>} como passthrough generico mientras el
 * contrato del servicio de usuarios no este cerrado; cuando se fije, se
 * reemplaza por DTOs tipados (record).
 *
 * Se usa {@code .block(TIMEOUT)} porque el BFF es una app servlet (MVC)
 * clasica: cada request ya corre en su propio hilo, asi que no se busca
 * no-bloqueo aqui, solo reusar WebClient como cliente HTTP moderno.
 */
@Component
public class UsersClient {

    /** Timeout maximo de espera para cada llamada al microservicio. */
    private static final Duration TIMEOUT = Duration.ofSeconds(6);

    private final WebClient usersWebClient;

    public UsersClient(@Qualifier("usersWebClient") WebClient usersWebClient) {
        this.usersWebClient = usersWebClient;
    }

    /**
     * Obtiene el perfil del usuario autenticado (token propagado).
     * ⚠️ Ruta supuesta: ajustar si el contrato real del servicio difiere.
     */
    public Map<String, Object> me() {
        return usersWebClient.get()
                .uri("/api/users/me")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() { })
                .block(TIMEOUT);
    }

    /**
     * Lista todos los usuarios (requiere rol ADMIN).
     */
    public List<Map<String, Object>> findAll() {
        return usersWebClient.get()
                .uri("/api/users")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() { })
                .block(TIMEOUT);
    }

    /**
     * Obtiene un usuario por su ID (requiere rol ADMIN).
     */
    public Map<String, Object> findById(String id) {
        return usersWebClient.get()
                .uri("/api/users/{id}", id)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() { })
                .block(TIMEOUT);
    }

    /**
     * Registra un nuevo usuario. No requiere token (endpoint publico).
     * ⚠️ Campos supuestos: ajustar si el contrato real del servicio difiere.
     */
    public Map<String, Object> register(Map<String, Object> body) {
        return usersWebClient.post()
                .uri("/api/users")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() { })
                .block(TIMEOUT);
    }

    /**
     * Actualiza datos de un usuario (requiere rol ADMIN).
     */
    public Map<String, Object> update(String id, Map<String, Object> body) {
        return usersWebClient.put()
                .uri("/api/users/{id}", id)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() { })
                .block(TIMEOUT);
    }

    /**
     * Elimina un usuario (requiere rol ADMIN).
     */
    public void delete(String id) {
        usersWebClient.delete()
                .uri("/api/users/{id}", id)
                .retrieve()
                .toBodilessEntity()
                .block(TIMEOUT);
    }
}
