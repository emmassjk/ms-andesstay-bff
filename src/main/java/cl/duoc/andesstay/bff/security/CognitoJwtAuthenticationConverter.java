package cl.duoc.andesstay.bff.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Cognito entrega los grupos del usuario (donde se manejan los roles,
 * p.ej. "ADMIN" o "AUDITOR") en el claim "cognito:groups" del token.
 * Este converter mapea ese claim a GrantedAuthority con prefijo ROLE_ para
 * poder usar hasRole(...)/hasAnyRole(...) en SecurityConfig.
 *
 * Ejemplo de claim en el token:
 *   "cognito:groups": ["ADMIN"]  ->  authority "ROLE_ADMIN"
 *
 * Los nombres de grupo deben coincidir con los del User Pool compartido por
 * catalog, audit y el resto de los microservicios.
 */
@Component
public class CognitoJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String GROUPS_CLAIM = "cognito:groups";
    private static final String ROLE_PREFIX = "ROLE_";

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractRoles(jwt);
        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }

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
}
