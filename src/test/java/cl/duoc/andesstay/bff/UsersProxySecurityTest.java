package cl.duoc.andesstay.bff;

import cl.duoc.andesstay.bff.client.UsersClient;
import cl.duoc.andesstay.bff.config.SecurityConfig;
import cl.duoc.andesstay.bff.controller.UsersProxyController;
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

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de la capa de seguridad del BFF sobre las rutas de usuarios:
 *  - sin token                         -> 401
 *  - GET /api/users/me con token       -> 200 (cualquier usuario autenticado)
 *  - GET /api/users con ROLE_AUDITOR   -> 403 (necesita ADMIN)
 *  - GET /api/users con ROLE_ADMIN     -> 200
 *  - POST /api/users con token         -> 201 (registro, authenticated)
 *  - DELETE /api/users/abc con ADMIN   -> 204
 *  - DELETE /api/users/abc sin ADMIN   -> 403
 *
 * Mismo patron que CatalogProxySecurityTest.
 */
@WebMvcTest(UsersProxyController.class)
@Import({
        SecurityConfig.class,
        CognitoJwtAuthenticationConverter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
class UsersProxySecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private UsersClient usersClient;

    @Test
    void sinTokenDeberiaResponder401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMeConTokenSinGruposDeberiaResponder200() throws Exception {
        when(usersClient.me()).thenReturn(Map.of("id", "abc", "email", "test@test.com"));

        mockMvc.perform(get("/api/users/me").with(jwt()))
                .andExpect(status().isOk());
    }

    @Test
    void getListadoConRolAuditorDeberiaResponder403() throws Exception {
        mockMvc.perform(get("/api/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_AUDITOR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getListadoConRolAdminDeberiaResponder200() throws Exception {
        mockMvc.perform(get("/api/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void postRegistroConTokenSinGruposDeberiaResponder201() throws Exception {
        mockMvc.perform(post("/api/users")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Juan","email":"juan@test.com","password":"12345678"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void deleteConRolAdminDeberiaResponder204() throws Exception {
        mockMvc.perform(delete("/api/users/abc")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteSinRolAdminDeberiaResponder403() throws Exception {
        mockMvc.perform(delete("/api/users/abc")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_HUESPED"))))
                .andExpect(status().isForbidden());
    }
}

