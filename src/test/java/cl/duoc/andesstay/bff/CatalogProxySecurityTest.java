package cl.duoc.andesstay.bff;

import cl.duoc.andesstay.bff.client.CatalogClient;
import cl.duoc.andesstay.bff.controller.CatalogProxyController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas basicas de la capa de seguridad del BFF:
 *  - sin token -> 401
 *  - token valido pero sin el rol requerido -> 403
 *  - token valido con rol ADMIN -> pasa la autorizacion (200)
 *
 * El JwtDecoder se reemplaza por un mock para no depender de Azure AD real
 * en la prueba unitaria; lo que se prueba es la configuracion de
 * SecurityConfig (reglas por rol), no la validez criptografica del token.
 */
@WebMvcTest(CatalogProxyController.class)
class CatalogProxySecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private CatalogClient catalogClient;

    @Test
    void sinTokenDeberiaResponder401() throws Exception {
        mockMvc.perform(get("/api/catalog/units"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenValidoSinRolAdminNiClienteDeberiaResponder403() throws Exception {
        when(catalogClient.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/catalog/units")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwtSinRoles())
                                .authorities(java.util.Collections.emptyList())))
                .andExpect(status().isForbidden());
    }

    @Test
    void tokenValidoConRolAdminDeberiaPermitirLectura() throws Exception {
        when(catalogClient.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/catalog/units")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    private Jwt jwtSinRoles() {
        return Jwt.withTokenValue("token-de-prueba")
                .header("alg", "none")
                .claim("sub", "usuario-de-prueba")
                .claim("aud", "api://andesstay-bff")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }
}
