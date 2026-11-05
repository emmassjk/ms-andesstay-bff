package cl.duoc.andesstay.bff.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO de entrada para el login de un usuario. El microservicio de usuarios
 * se encarga de autenticar contra Cognito y devolver los tokens (id_token,
 * access_token, refresh_token) al frontend.
 */
public record UserLoginRequestDto(
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email debe tener un formato valido")
        String email,

        @NotBlank(message = "La password es obligatoria")
        String password
) {}

