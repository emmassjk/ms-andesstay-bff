package cl.duoc.andesstay.bff.controller;

import cl.duoc.andesstay.bff.client.UsersClient;
import cl.duoc.andesstay.bff.dto.AuthResponseDto;
import cl.duoc.andesstay.bff.dto.UserLoginRequestDto;
import cl.duoc.andesstay.bff.dto.UserRegisterRequestDto;
import cl.duoc.andesstay.bff.dto.UserResponseDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Puerta de entrada del frontend Angular hacia el dominio de usuarios
 * (ms-andesstay-users). Actua como proxy sin logica de negocio: valida
 * la entrada y reenvia hacia el microservicio de usuarios via UsersClient.
 *
 * <h3>Endpoints publicos (sin token):</h3>
 * <ul>
 *   <li>{@code POST /api/users/register} — Registro de nuevo huesped</li>
 *   <li>{@code POST /api/users/login} — Login OIDC (devuelve tokens Cognito)</li>
 * </ul>
 *
 * <h3>Endpoints protegidos (requieren access token valido):</h3>
 * <ul>
 *   <li>{@code GET /api/users/me} — Perfil del usuario autenticado</li>
 *   <li>{@code GET /api/users} — Listar usuarios (ADMIN/RECEPCIONISTA)</li>
 *   <li>{@code GET /api/users/{id}} — Detalle de un usuario (ADMIN/RECEPCIONISTA)</li>
 *   <li>{@code PUT /api/users/{id}} — Actualizar perfil</li>
 * </ul>
 *
 * La autorizacion por rol se resuelve en SecurityConfig; si este controller
 * se ejecuta, es porque las condiciones de seguridad ya se cumplieron.
 */
@RestController
@RequestMapping("/api/users")
public class UsersProxyController {

    private final UsersClient usersClient;

    public UsersProxyController(UsersClient usersClient) {
        this.usersClient = usersClient;
    }

    /**
     * Registro de un nuevo usuario. Endpoint publico (permitAll en SecurityConfig).
     * El microservicio de usuarios crea la cuenta en Cognito y persiste el perfil.
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponseDto register(@Valid @RequestBody UserRegisterRequestDto request) {
        return usersClient.register(request);
    }

    /**
     * Login (flujo USER_PASSWORD_AUTH de Cognito). Endpoint publico.
     * Devuelve los tokens OIDC (access_token, id_token, refresh_token)
     * para que el frontend Angular los almacene y use en llamadas posteriores.
     */
    @PostMapping("/login")
    public AuthResponseDto login(@Valid @RequestBody UserLoginRequestDto request) {
        return usersClient.login(request);
    }

    /**
     * Obtiene el perfil del usuario autenticado (el "sub" del token).
     * Requiere un access token valido.
     */
    @GetMapping("/me")
    public UserResponseDto myProfile() {
        return usersClient.getMyProfile();
    }

    /**
     * Lista todos los usuarios registrados. Solo ADMIN o RECEPCIONISTA.
     */
    @GetMapping
    public List<UserResponseDto> findAll() {
        return usersClient.findAll();
    }

    /**
     * Obtiene el detalle de un usuario por ID. Solo ADMIN o RECEPCIONISTA.
     */
    @GetMapping("/{id}")
    public UserResponseDto findById(@PathVariable String id) {
        return usersClient.findById(id);
    }

    /**
     * Actualiza el perfil de un usuario. El propio usuario o un ADMIN.
     */
    @PutMapping("/{id}")
    public UserResponseDto update(@PathVariable String id,
                                   @Valid @RequestBody UserRegisterRequestDto request) {
        return usersClient.update(id, request);
    }
}

