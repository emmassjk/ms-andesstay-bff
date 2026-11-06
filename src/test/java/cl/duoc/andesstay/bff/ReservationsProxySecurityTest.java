package cl.duoc.andesstay.bff;

import cl.duoc.andesstay.bff.client.ReservationClient;
import cl.duoc.andesstay.bff.config.SecurityConfig;
import cl.duoc.andesstay.bff.controller.ReservationsProxyController;
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

import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de la capa de seguridad del BFF sobre las rutas de reservas:
 *  - sin token                       -> 401
 *  - token valido, cualquier grupo   -> puede LEER (200)
 *  - POST con ROLE_HUESPED           -> 201 (puede crear)
 *  - POST con ROLE_AUDITOR           -> 403 (no puede crear)
 *  - PUT con ROLE_HUESPED            -> 403 (no puede confirmar)
 *  - PUT con ROLE_RECEPCIONISTA      -> 200 (puede confirmar)
 *  - DELETE con ROLE_RECEPCIONISTA   -> 403 (no puede eliminar)
 *  - DELETE con ROLE_ADMIN           -> 204 (puede eliminar)
 *
 * Mismo patron que CatalogProxySecurityTest: @WebMvcTest con SecurityConfig
 * real, JwtDecoder mock y el cliente mock.
 */
@WebMvcTest(ReservationsProxyController.class)
@Import({
        SecurityConfig.class,
        CognitoJwtAuthenticationConverter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
class ReservationsProxySecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private ReservationClient reservationClient;

    /**
     * Body JSON valido para crear una reserva: unitId, checkIn/checkOut futuros, guestsCount.
     * Se generan fechas futuras dinamicamente para que @FutureOrPresent no falle.
     */
    private String reservationJson() {
        LocalDate checkIn = LocalDate.now().plusDays(10);
        LocalDate checkOut = checkIn.plusDays(3);
        return """
                {"unitId":1,"checkIn":"%s","checkOut":"%s","guestsCount":2}
                """.formatted(checkIn, checkOut);
    }

    @Test
    void sinTokenDeberiaResponder401() throws Exception {
        mockMvc.perform(get("/api/reservations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenSinGruposDeberiaPoderLeer() throws Exception {
        mockMvc.perform(get("/api/reservations").with(jwt()))
                .andExpect(status().isOk());
    }

    @Test
    void postConRolHuespedDeberiaPoderCrear() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_HUESPED")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson()))
                .andExpect(status().isCreated());
    }

    @Test
    void postConRolAuditorNoDeberiaPoderCrear() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_AUDITOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    void putConfirmarConRolHuespedNoDeberiaPoder() throws Exception {
        mockMvc.perform(put("/api/reservations/1/confirmar")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_HUESPED"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void putConfirmarConRolRecepcionistaDeberiaPoder() throws Exception {
        mockMvc.perform(put("/api/reservations/1/confirmar")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_RECEPCIONISTA"))))
                .andExpect(status().isOk());
    }

    @Test
    void deleteConRolRecepcionistaNoDeberiaPoder() throws Exception {
        mockMvc.perform(delete("/api/reservations/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_RECEPCIONISTA"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteConRolAdminDeberiaPoder() throws Exception {
        mockMvc.perform(delete("/api/reservations/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNoContent());
    }
}

