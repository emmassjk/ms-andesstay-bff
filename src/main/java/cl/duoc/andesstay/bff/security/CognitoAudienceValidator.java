package cl.duoc.andesstay.bff.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Los access tokens de Cognito no traen el claim estandar "aud"; en su
 * lugar traen "client_id" con el App Client ID que emitio el token.
 * Este validador es el mismo que usan ms-andesstay-catalog y
 * ms-andesstay-audit.
 *
 * Como los ID tokens de Cognito traen "aud" pero NO "client_id", este
 * validador tambien rechaza un ID token enviado por error como Bearer.
 */
public class CognitoAudienceValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error ERROR = new OAuth2Error(
            "invalid_token", "El token no fue emitido para este client_id", null);

    private final String expectedClientId;

    public CognitoAudienceValidator(String expectedClientId) {
        this.expectedClientId = expectedClientId;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String clientId = jwt.getClaimAsString("client_id");
        if (clientId != null && clientId.equals(expectedClientId)) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(ERROR);
    }
}
