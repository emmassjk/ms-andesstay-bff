package cl.duoc.andesstay.bff.dto;

/**
 * DTO con los tokens devueltos por Cognito tras un login exitoso. El
 * microservicio de usuarios orquesta el flujo OIDC (InitiateAuth) y
 * devuelve los tokens para que el frontend Angular los almacene.
 */
public record AuthResponseDto(
        String accessToken,
        String idToken,
        String refreshToken,
        String tokenType,
        int expiresIn
) {}

