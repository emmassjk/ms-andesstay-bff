package cl.duoc.andesstay.bff.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Valida que el claim "aud" del JWT emitido por Azure AD corresponda al
 * Application (client) ID de la API que expone este BFF.
 *
 * Sin este validador, cualquier token valido emitido por el mismo tenant
 * (aunque haya sido emitido para OTRA aplicacion) seria aceptado, lo cual
 * es una vulnerabilidad de "confused deputy". El issuer se valida aparte
 * (ver JwtConfig), asi que ambos validators se combinan.
 */
public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INVALID_AUDIENCE_ERROR =
            new OAuth2Error("invalid_token", "El token no contiene el audience esperado para esta API", null);

    private final String expectedAudience;

    public AudienceValidator(String expectedAudience) {
        this.expectedAudience = expectedAudience;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        if (jwt.getAudience() != null && jwt.getAudience().contains(expectedAudience)) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(INVALID_AUDIENCE_ERROR);
    }
}
