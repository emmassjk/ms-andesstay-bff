package cl.duoc.andesstay.bff.config;

import cl.duoc.andesstay.bff.security.CognitoJwtAuthenticationConverter;
import cl.duoc.andesstay.bff.security.RestAccessDeniedHandler;
import cl.duoc.andesstay.bff.security.RestAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuracion central de seguridad del BFF.
 *
 * Rol del BFF en la arquitectura (Angular + Cognito) -> (AWS API Gateway) -> BFF -> microservicios:
 *   1. Verifica que exista un access token valido emitido por el User Pool
 *      de Cognito (firma, issuer, client_id y vigencia; ver JwtConfig) para
 *      CUALQUIER endpoint de negocio.
 *   2. Aplica autorizacion por rol (claim "cognito:groups" del token, ver
 *      CognitoJwtAuthenticationConverter), con las mismas reglas que los
 *      microservicios de dominio:
 *
 *        GET  /api/catalog/units/**        -> cualquier usuario autenticado
 *        POST/PUT/DELETE catalogo          -> solo ADMIN
 *        GET  /api/audit/**                -> ADMIN o AUDITOR
 *        GET  /api/reservations/**         -> cualquier usuario autenticado
 *        POST /api/reservations/**         -> RECEPCIONISTA o ADMIN
 *        PUT/PATCH /api/reservations/**    -> RECEPCIONISTA o ADMIN
 *        DELETE /api/reservations/**       -> solo ADMIN
 *        POST /api/users/register          -> publico (registro)
 *        POST /api/users/login             -> publico (login OIDC)
 *        GET  /api/users/me                -> cualquier usuario autenticado
 *        GET  /api/users/**                -> ADMIN o RECEPCIONISTA
 *
 *   3. Solo si ambas condiciones se cumplen, permite que la peticion llegue
 *      al controller que reenvia la llamada al microservicio de dominio
 *      (junto con el mismo token, ver BearerTokenPropagationFilter).
 *
 * Antes esta cadena tenia anyRequest().permitAll() y el resource server no
 * estaba conectado, asi que el BFF no exigia token en ninguna ruta. Ahora
 * .oauth2ResourceServer(...) SI queda conectado al filterChain.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    private final CognitoJwtAuthenticationConverter cognitoJwtAuthenticationConverter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(CognitoJwtAuthenticationConverter cognitoJwtAuthenticationConverter,
                           RestAuthenticationEntryPoint authenticationEntryPoint,
                           RestAccessDeniedHandler accessDeniedHandler) {
        this.cognitoJwtAuthenticationConverter = cognitoJwtAuthenticationConverter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Sin esto, cualquier error que Spring MVC resuelva con sendError() (p.ej. JSON mal
                        // formado) se redirige a /error, que llega sin token y terminaria como 401/403.
                        .requestMatchers("/error").permitAll()
                        // health publico para el API Gateway / balanceador si se agrega actuator
                        .requestMatchers("/actuator/health").permitAll()

                        // --- Usuarios: registro y login son publicos (flujo OIDC/Cognito) ---
                        .requestMatchers(HttpMethod.POST, "/api/users/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users/login").permitAll()
                        .requestMatchers(HttpMethod.GET,  "/api/users/me").authenticated()
                        .requestMatchers(HttpMethod.GET,  "/api/users/**").hasAnyRole("ADMIN", "RECEPCIONISTA")
                        .requestMatchers(HttpMethod.PUT,  "/api/users/**").authenticated()

                        // --- Catalogo (mismas reglas que ms-andesstay-catalog) ---
                        .requestMatchers(HttpMethod.GET, "/api/catalog/units/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/catalog/units/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/catalog/units/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/catalog/units/**").hasRole("ADMIN")

                        // --- Auditoria (mismas reglas que ms-andesstay-audit) ---
                        .requestMatchers("/api/audit/**").hasAnyRole("ADMIN", "AUDITOR")

                        // --- Reservas (mismas reglas que ms-andesstay-reservations) ---
                        // GET: cualquier usuario autenticado (huesped consulta sus reservas,
                        //      recepcionista y admin consultan todas).
                        .requestMatchers(HttpMethod.GET,    "/api/reservations/**").authenticated()
                        // POST: crear reserva -> huesped, recepcionista o admin.
                        .requestMatchers(HttpMethod.POST,   "/api/reservations/**").hasAnyRole("HUESPED", "RECEPCIONISTA", "ADMIN")
                        // PUT/PATCH: modificar reserva -> recepcionista o admin.
                        .requestMatchers(HttpMethod.PUT,    "/api/reservations/**").hasAnyRole("RECEPCIONISTA", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH,  "/api/reservations/**").hasAnyRole("RECEPCIONISTA", "ADMIN")
                        // DELETE: cancelar/eliminar reserva -> solo admin.
                        .requestMatchers(HttpMethod.DELETE, "/api/reservations/**").hasRole("ADMIN")

                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(cognitoJwtAuthenticationConverter)));

        return http.build();
    }

    /**
     * Permite que Angular (levantado en otro origen/puerto) consuma el BFF
     * enviando el header Authorization: Bearer &lt;access_token&gt;.
     *
     * Se permiten todos los origenes configurados (o "*" para desarrollo),
     * los metodos HTTP basicos (GET, POST, PUT, DELETE, PATCH, OPTIONS),
     * todos los headers (incluido Authorization) y se expone el header
     * Authorization para que el frontend pueda leerlo en las respuestas.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Origenes: soporta multiples separados por coma en la property
        List<String> origins = List.of(allowedOrigins.split(","));
        configuration.setAllowedOrigins(origins);

        // Metodos HTTP permitidos (incluye PATCH y OPTIONS para preflight)
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // Todos los headers permitidos (Authorization, Content-Type, X-Requested-With, etc.)
        configuration.setAllowedHeaders(List.of("*"));

        // Exponer el header Authorization para que el frontend Angular pueda leerlo
        configuration.setExposedHeaders(List.of("Authorization"));

        // Permitir envio de cookies/credenciales (necesario para flujos OIDC)
        configuration.setAllowCredentials(true);

        // Cache de preflight de 1 hora para reducir llamadas OPTIONS
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
