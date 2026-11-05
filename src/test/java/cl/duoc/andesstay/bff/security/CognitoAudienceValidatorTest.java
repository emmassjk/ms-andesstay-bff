package cl.duoc.andesstay.bff.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class CognitoAudienceValidatorTest {

    private static final String CLIENT_ID = "app-client-esperado";

    private final CognitoAudienceValidator validator = new CognitoAudienceValidator(CLIENT_ID);

    @Test
    void aceptaAccessTokenEmitidoParaElAppClientEsperado() {
        Jwt accessToken = jwtBuilder().claim("client_id", CLIENT_ID).build();

        assertThat(validator.validate(accessToken).hasErrors()).isFalse();
    }

    @Test
    void rechazaTokenDeOtroAppClient() {
        Jwt otroClient = jwtBuilder().claim("client_id", "otro-app-client").build();

        assertThat(validator.validate(otroClient).hasErrors()).isTrue();
    }

    @Test
    void rechazaIdTokenQueSoloTraeAud() {
        Jwt idToken = jwtBuilder().claim("aud", CLIENT_ID).build();

        assertThat(validator.validate(idToken).hasErrors()).isTrue();
    }

    @Test
    void rechazaTokenExpirado() {
        Jwt tokenExpirado = Jwt.withTokenValue("token-de-prueba")
                .header("alg", "none")
                .claim("sub", "usuario-de-prueba")
                .claim("client_id", CLIENT_ID)
                .issuedAt(Instant.now().minusSeconds(7200))
                .expiresAt(Instant.now().minusSeconds(3600))
                .build();

        assertThat(validator.validate(tokenExpirado).hasErrors()).isTrue();
    }

    @Test
    void aceptaTokenNoExpiradoConClientIdCorrecto() {
        Jwt tokenValido = jwtBuilder()
                .claim("client_id", CLIENT_ID)
                .build();

        assertThat(validator.validate(tokenValido).hasErrors()).isFalse();
    }

    private Jwt.Builder jwtBuilder() {
        return Jwt.withTokenValue("token-de-prueba")
                .header("alg", "none")
                .claim("sub", "usuario-de-prueba")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600));
    }
}
