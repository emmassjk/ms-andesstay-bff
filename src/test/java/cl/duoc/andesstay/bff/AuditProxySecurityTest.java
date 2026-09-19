package cl.duoc.andesstay.bff;

import cl.duoc.andesstay.bff.client.AuditClient;
import cl.duoc.andesstay.bff.config.SecurityConfig;
import cl.duoc.andesstay.bff.controller.AuditProxyController;
import cl.duoc.andesstay.bff.security.CognitoJwtAuthenticationConverter;
import cl.duoc.andesstay.bff.security.RestAccessDeniedHandler;
import cl.duoc.andesstay.bff.security.RestAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /api/audit solo lo pueden ver ADMIN y AUDITOR (misma regla que
 * ms-andesstay-audit); cualquier otro usuario autenticado recibe 403.
 */
@WebMvcTest(AuditProxyController.class)
@Import({
        SecurityConfig.class,
        CognitoJwtAuthenticationConverter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
class AuditProxySecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private AuditClient auditClient;

    @Test
    void sinTokenDeberiaResponder401() throws Exception {
        mockMvc.perform(get("/api/audit"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(auditClient);
    }

    @Test
    void tokenSinRolAdminNiAuditorDeberiaResponder403() throws Exception {
        mockMvc.perform(get("/api/audit")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(auditClient);
    }

    @Test
    void rolAuditorDeberiaPoderConsultar() throws Exception {
        mockMvc.perform(get("/api/audit")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_AUDITOR"))))
                .andExpect(status().isOk());
    }

    @Test
    void rolAdminDeberiaPoderConsultar() throws Exception {
        mockMvc.perform(get("/api/audit")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void tamanoDePaginaSeAcotaAlMaximoPermitido() throws Exception {
        mockMvc.perform(get("/api/audit")
                        .param("page", "-3")
                        .param("size", "500")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());

        verify(auditClient).search(isNull(), isNull(), isNull(), isNull(), eq(0), eq(100));
    }
}
