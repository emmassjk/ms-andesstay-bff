package cl.duoc.andesstay.bff.config;

import cl.duoc.andesstay.bff.security.CognitoAudienceValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Construye el JwtDecoder del BFF, validando tokens emitidos por un User
 * Pool de AWS Cognito (el mismo que usan ms-andesstay-catalog y
 * ms-andesstay-audit).
 *
 * NimbusJwtDecoder ya valida por si solo, contra las llaves publicas (JWKS)
 * del User Pool resueltas desde el documento de descubrimiento OIDC:
 *   - la FIRMA del token (que fue emitido por Cognito y no fue alterado)
 *   - la VIGENCIA (exp/nbf)
 *
 * A eso se le agregan explicitamente:
 *   - validacion de ISSUER (JwtValidators.createDefaultWithIssuer)
 *   - validacion del App Client (CognitoAudienceValidator)
 *
 * IMPORTANTE - Cognito tiene dos tipos de token distintos:
 *   - ID token: trae "aud" (el App Client ID); es para identificar al usuario
 *     en el propio frontend.
 *   - Access token: NO trae "aud", trae "client_id". Es el que el frontend
 *     debe mandar como Bearer al BFF.
 * Por eso se valida "client_id" y no "aud".
 *
 * issuer-uri esperado (ver application.yml):
 *   https://cognito-idp.{region}.amazonaws.com/{userPoolId}
 * Spring resuelve el JWKS en {issuer-uri}/.well-known/jwks.json
 *
 * Si cualquiera de estas validaciones falla, NimbusJwtDecoder lanza
 * JwtValidationException, que el resource server traduce en un 401 manejado
 * por RestAuthenticationEntryPoint.
 */
@Configuration
public class JwtConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Value("${cognito.app-client-id}")
    private String expectedClientId;

    @Bean
    public JwtDecoder jwtDecoder() {
        if (issuerUri != null && issuerUri.contains("CHANGE_ME")) {
            log.warn("El issuer-uri contiene el placeholder 'CHANGE_ME'. Se usa un JwtDecoder de respaldo "
                    + "que rechaza todos los tokens (401) para permitir el inicio de la app en desarrollo local. "
                    + "Configura COGNITO_REGION, COGNITO_USER_POOL_ID y COGNITO_APP_CLIENT_ID para conectarte a Cognito.");
            return token -> {
                throw new JwtException("No se ha configurado un User Pool de Cognito valido en las variables de entorno.");
            };
        }

        NimbusJwtDecoder jwtDecoder = (NimbusJwtDecoder) JwtDecoders.fromIssuerLocation(issuerUri);

        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> withClientId = new CognitoAudienceValidator(expectedClientId);
        OAuth2TokenValidator<Jwt> combinedValidator =
                new DelegatingOAuth2TokenValidator<>(withIssuer, withClientId);

        jwtDecoder.setJwtValidator(combinedValidator);
        return jwtDecoder;
    }
}
