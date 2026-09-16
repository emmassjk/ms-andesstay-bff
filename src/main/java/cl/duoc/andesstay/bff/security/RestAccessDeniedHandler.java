package cl.duoc.andesstay.bff.security;

import cl.duoc.andesstay.bff.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Se ejecuta cuando el JWT es valido (firma, issuer, audience y vigencia
 * correctas) pero el rol del usuario no autoriza la operacion solicitada
 * (por ejemplo, un CLIENTE intentando crear/editar/eliminar una unidad).
 * Responde 403, no 401: el token esta bien, el permiso no alcanza.
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                        AccessDeniedException accessDeniedException) throws IOException, ServletException {

        ErrorResponse body = ErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                "forbidden",
                "No tienes permisos (rol) suficientes para realizar esta operacion.",
                request.getRequestURI());

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
