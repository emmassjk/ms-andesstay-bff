package cl.duoc.andesstay.bff.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Convierte un JWT de Cognito en un {@link JwtAuthenticationToken} con los
 * roles/authorities correctos para que Spring Security pueda evaluar
 * {@code hasRole(...)} / {@code hasAnyRole(...)}.
 *
 * <h3>Fuentes de roles (en orden de prioridad):</h3>
 * <ol>
 *   <li><strong>cognito:groups</strong> — Grupos del User Pool de Cognito
 *       (p.ej. "ADMIN", "AUDITOR", "RECEPCIONISTA", "HUESPED"). Es el claim
 *       principal para el control de acceso.</li>
 *   <li><strong>scope</strong> — Scopes OAuth2 del token (p.ej. "openid",
 *       "profile"). Se mapean con prefijo {@code SCOPE_} como es estandar
 *       en Spring Security.</li>
 * </ol>
 *
 * Ejemplo de mapeo:
 * <pre>
 *   "cognito:groups": ["ADMIN"]          -> authority "ROLE_ADMIN"
 *   "scope": "openid profile"            -> authorities "SCOPE_openid", "SCOPE_profile"
 * </pre>
 *
 * Los nombres de grupo deben coincidir con los del User Pool compartido por
 * catalog, audit, reservations y el resto de los microservicios.
 */
@Component
public class CognitoJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String GROUPS_CLAIM = "cognito:groups";
    private static final String ROLE_PREFIX = "ROLE_";
    private static final String SCOPE_PREFIX = "SCOPE_";

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();

        // Extraer roles de cognito:groups
        authorities.addAll(extractRoles(jwt));

        // Extraer scopes OAuth2
        authorities.addAll(extractScopes(jwt));

        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }

    /**
     * Extrae los grupos de Cognito (claim "cognito:groups") y los mapea
     * a authorities con prefijo ROLE_ en mayusculas.
     */
    private Collection<GrantedAuthority> extractRoles(Jwt jwt) {
        List<String> groups = jwt.getClaimAsStringList(GROUPS_CLAIM);
        if (groups == null) {
            return List.of();
        }
        return groups.stream()
                .filter(Objects::nonNull)
                .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role.toUpperCase()))
                .collect(Collectors.toList());
    }

    /**
     * Extrae los scopes OAuth2 del claim "scope" (string separado por
     * espacios) y los mapea a authorities con prefijo SCOPE_.
     */
    private Collection<GrantedAuthority> extractScopes(Jwt jwt) {
        String scope = jwt.getClaimAsString("scope");
        if (scope == null || scope.isBlank()) {
            return List.of();
        }
        return List.of(scope.split("\\s+")).stream()
                .filter(s -> !s.isBlank())
                .map(s -> new SimpleGrantedAuthority(SCOPE_PREFIX + s))
                .collect(Collectors.toList());
    }
}
