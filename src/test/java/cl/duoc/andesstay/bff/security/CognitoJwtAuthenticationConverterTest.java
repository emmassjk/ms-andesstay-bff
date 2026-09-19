package cl.duoc.andesstay.bff.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CognitoJwtAuthenticationConverterTest {

    private final CognitoJwtAuthenticationConverter converter = new CognitoJwtAuthenticationConverter();

    @Test
    void mapeaGruposDeCognitoARolesConPrefijoEnMayusculas() {
        Jwt jwt = baseJwt()
                .claim("cognito:groups", List.of("admin", "Auditor"))
                .build();

        List<String> authorities = converter.convert(jwt).getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        assertThat(authorities).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_AUDITOR");
    }

    @Test
    void sinClaimDeGruposNoOtorgaRoles() {
        Jwt jwt = baseJwt().build();

        assertThat(converter.convert(jwt).getAuthorities()).isEmpty();
    }

    private Jwt.Builder baseJwt() {
        return Jwt.withTokenValue("token-de-prueba")
                .header("alg", "none")
                .claim("sub", "usuario-de-prueba")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600));
    }
}
