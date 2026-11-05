package cl.duoc.andesstay.bff.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para el registro de un nuevo usuario via el microservicio
 * de usuarios. Los campos minimos son nombre, email y password; el servicio
 * de usuarios se encarga de crear la cuenta en Cognito y persistir el perfil.
 */
public record UserRegisterRequestDto(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @NotBlank(message = "El apellido es obligatorio")
        String apellido,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email debe tener un formato valido")
        String email,

        @NotBlank(message = "La password es obligatoria")
        @Size(min = 8, message = "La password debe tener al menos 8 caracteres")
        String password,

        String telefono
) {}

