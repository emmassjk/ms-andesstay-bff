package cl.duoc.andesstay.bff;

import cl.duoc.andesstay.bff.client.CatalogClient;
import cl.duoc.andesstay.bff.config.SecurityConfig;
import cl.duoc.andesstay.bff.controller.CatalogProxyController;
import cl.duoc.andesstay.bff.security.CognitoJwtAuthenticationConverter;
import cl.duoc.andesstay.bff.security.RestAccessDeniedHandler;
import cl.duoc.andesstay.bff.security.RestAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de la capa de seguridad del BFF sobre las rutas de catalogo, con las
 * mismas reglas que ms-andesstay-catalog:
 *  - sin token                       -> 401
 *  - token valido, cualquier grupo   -> puede LEER (200)
 *  - token valido sin grupo ADMIN    -> NO puede escribir (403)
 *  - token valido con grupo ADMIN    -> puede escribir
 *
 * El JwtDecoder se reemplaza por un mock para no depender de Cognito real; lo
 * que se prueba es SecurityConfig (reglas por rol), no la validez criptografica
 * del token. SecurityConfig y los handlers se importan explicitamente porque
 * @WebMvcTest no carga clases @Configuration/@Component por si solo (sin el
 * import se probaria la seguridad por defecto de Spring Boot, no la del BFF).
 */
@WebMvcTest(CatalogProxyController.class)
@Import({
        SecurityConfig.class,
        CognitoJwtAuthenticationConverter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
class CatalogProxySecurityTest {

    private static final String UNIT_JSON = """
            {"name":"Cabana Lago 1","type":"CABANA","capacity":4,"pricePerNight":45000,"available":true}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private CatalogClient catalogClient;

    @Test
    void sinTokenDeberiaResponder401ConCuerpoJson() throws Exception {
        mockMvc.perform(get("/api/catalog/units"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("unauthorized"));
    }

    @Test
    void tokenValidoSinGruposDeberiaPoderLeer() throws Exception {
        when(catalogClient.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/catalog/units").with(jwt()))
                .andExpect(status().isOk());
    }

    @Test
    void tokenValidoSinRolAdminNoDeberiaPoderCrear() throws Exception {
        mockMvc.perform(post("/api/catalog/units")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_AUDITOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UNIT_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("forbidden"));
    }

    @Test
    void tokenConRolAdminDeberiaPoderCrear() throws Exception {
        mockMvc.perform(post("/api/catalog/units")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UNIT_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    void tokenSinRolAdminNoDeberiaPoderEliminar() throws Exception {
        mockMvc.perform(delete("/api/catalog/units/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_AUDITOR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void tokenConRolAdminDeberiaPoderEliminar() throws Exception {
        mockMvc.perform(delete("/api/catalog/units/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNoContent());
    }
}
