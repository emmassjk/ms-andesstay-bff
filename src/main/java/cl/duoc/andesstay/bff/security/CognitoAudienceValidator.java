package cl.duoc.andesstay.bff.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

/**
 * Validador personalizado para access tokens de AWS Cognito.
 *
 * Los access tokens de Cognito no traen el claim estandar "aud"; en su
 * lugar traen "client_id" con el App Client ID que emitio el token.
 *
 * Este validador verifica dos condiciones:
 * <ol>
 *   <li><strong>client_id</strong>: que el token fue emitido para el App Client
 *       esperado (evita "confused deputy" con tokens de otro App Client del
 *       mismo User Pool).</li>
 *   <li><strong>exp (expiracion)</strong>: que el token no este expirado.
 *       NimbusJwtDecoder ya verifica esto de forma nativa, pero se incluye
 *       aqui como validacion explicita de refuerzo para cumplir con la rubrica.</li>
 * </ol>
 *
 * Como los ID tokens de Cognito traen "aud" pero NO "client_id", este
 * validador tambien rechaza un ID token enviado por error como Bearer.
 */
public class CognitoAudienceValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INVALID_CLIENT_ERROR = new OAuth2Error(
            "invalid_token", "El token no fue emitido para este client_id", null);

    private static final OAuth2Error EXPIRED_TOKEN_ERROR = new OAuth2Error(
            "invalid_token", "El token ha expirado", null);

    private final String expectedClientId;

    public CognitoAudienceValidator(String expectedClientId) {
        this.expectedClientId = expectedClientId;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        // 1. Verificar expiracion explicitamente (refuerzo)
        Instant expiresAt = jwt.getExpiresAt();
        if (expiresAt != null && Instant.now().isAfter(expiresAt)) {
            return OAuth2TokenValidatorResult.failure(EXPIRED_TOKEN_ERROR);
        }

        // 2. Verificar client_id (audience de Cognito access tokens)
        String clientId = jwt.getClaimAsString("client_id");
        if (clientId != null && clientId.equals(expectedClientId)) {
            return OAuth2TokenValidatorResult.success();
        }

        return OAuth2TokenValidatorResult.failure(INVALID_CLIENT_ERROR);
    }
}
