package cl.duoc.andesstay.bff.controller;

import cl.duoc.andesstay.bff.client.UsersClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Puerta de entrada del frontend Angular hacia el dominio de usuarios
 * (ms-andesstay-users). Actua como proxy sin logica de negocio: reenvia
 * las peticiones hacia el microservicio de usuarios via {@link UsersClient}.
 *
 * <h3>Endpoints publicos (sin token):</h3>
 * <ul>
 *   <li>{@code POST /api/users} — Registro de nuevo usuario</li>
 * </ul>
 *
 * <h3>Endpoints protegidos (requieren access token valido):</h3>
 * <ul>
 *   <li>{@code GET  /api/users/me}   — Perfil del usuario autenticado</li>
 *   <li>{@code GET  /api/users}      — Listar usuarios (ADMIN)</li>
 *   <li>{@code GET  /api/users/{id}} — Detalle de un usuario (ADMIN)</li>
 *   <li>{@code PUT  /api/users/{id}} — Actualizar perfil (ADMIN)</li>
 *   <li>{@code DELETE /api/users/{id}} — Eliminar usuario (ADMIN)</li>
 * </ul>
 *
 * La autorizacion por rol se resuelve en SecurityConfig; si este controller
 * se ejecuta, es porque las condiciones de seguridad ya se cumplieron.
 *
 * Se usa {@code Map<String,Object>} como passthrough generico mientras el
 * contrato del servicio de usuarios no este cerrado.
 */
@RestController
@RequestMapping("/api/users")
public class UsersProxyController {

    private final UsersClient usersClient;

    public UsersProxyController(UsersClient usersClient) {
        this.usersClient = usersClient;
    }

    /**
     * Perfil del usuario autenticado (el "sub" del token).
     * Requiere un access token valido.
     */
    @GetMapping("/me")
    public Map<String, Object> me() {
        return usersClient.me();
    }

    /**
     * Lista todos los usuarios registrados. Solo ADMIN.
     */
    @GetMapping
    public List<Map<String, Object>> findAll() {
        return usersClient.findAll();
    }

    /**
     * Obtiene el detalle de un usuario por ID. Solo ADMIN.
     */
    @GetMapping("/{id}")
    public Map<String, Object> findById(@PathVariable String id) {
        return usersClient.findById(id);
    }

    /**
     * Registro de un nuevo usuario. Endpoint publico (authenticated en
     * SecurityConfig, pero no requiere rol especifico).
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> register(@RequestBody Map<String, Object> body) {
        return usersClient.register(body);
    }

    /**
     * Actualiza el perfil de un usuario. Solo ADMIN.
     */
    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable String id,
                                       @RequestBody Map<String, Object> body) {
        return usersClient.update(id, body);
    }

    /**
     * Elimina un usuario. Solo ADMIN.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        usersClient.delete(id);
        return ResponseEntity.noContent().build();
    }
}
