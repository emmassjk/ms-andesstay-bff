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

                        // --- Catalogo (mismas reglas que ms-andesstay-catalog) ---
                        .requestMatchers(HttpMethod.GET, "/api/catalog/units/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/catalog/units/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/catalog/units/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/catalog/units/**").hasRole("ADMIN")

                        // --- Auditoria (mismas reglas que ms-andesstay-audit) ---
                        .requestMatchers("/api/audit/**").hasAnyRole("ADMIN", "AUDITOR")

                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(cognitoJwtAuthenticationConverter)));

        return http.build();
    }

    /**
     * Permite que Angular (levantado en otro origen/puerto) consuma el BFF
     * enviando el header Authorization: Bearer <access_token>.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
