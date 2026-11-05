package cl.duoc.andesstay.bff.dto;

import java.time.Instant;
import java.util.List;

/**
 * DTO de salida con la informacion del usuario devuelta por el microservicio
 * de usuarios. Incluye datos de perfil y los grupos/roles de Cognito.
 */
public record UserResponseDto(
        String id,
        String nombre,
        String apellido,
        String email,
        String telefono,
        List<String> roles,
        Instant creadoEn
) {}

