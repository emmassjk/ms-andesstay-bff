package cl.duoc.andesstay.bff.config;

import cl.duoc.andesstay.bff.security.AudienceValidator;
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
 * Construye el JwtDecoder usado por el resource server.
 *
 * NimbusJwtDecoder ya valida por si solo, contra las llaves publicas (JWKS)
 * del IDaaS resueltas desde el documento de descubrimiento OIDC:
 *   - la FIRMA del token (que fue emitido por Azure AD y no fue alterado)
 *   - la VIGENCIA (exp/nbf)
 *
 * A eso se le agregan explicitamente:
 *   - validacion de ISSUER (JwtValidators.createDefaultWithIssuer)
 *   - validacion de AUDIENCE (AudienceValidator), que Spring no valida por defecto
 *
 * Si cualquiera de estas validaciones falla, NimbusJwtDecoder lanza
 * JwtValidationException, que el resource server traduce automaticamente en
 * un 401 manejado por RestAuthenticationEntryPoint.
 */
@Configuration
public class JwtConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Value("${azure.ad.audience}")
    private String expectedAudience;

    @Bean
    public JwtDecoder jwtDecoder() {
        if (issuerUri != null && issuerUri.contains("CHANGE_ME")) {
            System.out.println("[WARN] ATENCION: El issuer-uri contiene el placeholder 'CHANGE_ME'. Se usará un JwtDecoder de respaldo para permitir el inicio de la app en desarrollo local. Configura la variable de entorno AZURE_TENANT_ID para conectarte a Azure AD.");
            return token -> {
                throw new JwtException("No se ha configurado un AZURE_TENANT_ID valido en las variables de entorno.");
            };
        }

        NimbusJwtDecoder jwtDecoder = (NimbusJwtDecoder) JwtDecoders.fromIssuerLocation(issuerUri);

        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> withAudience = new AudienceValidator(expectedAudience);
        OAuth2TokenValidator<Jwt> combinedValidator =
                new DelegatingOAuth2TokenValidator<>(withIssuer, withAudience);

        jwtDecoder.setJwtValidator(combinedValidator);
        return jwtDecoder;
    }
}
