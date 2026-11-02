package ar.edu.tpi.commons.seguridad;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Convierte un JWT emitido por Keycloak en una autenticacion de Spring Security.
 *
 * <p>Keycloak publica los roles del realm en el claim {@code realm_access.roles}.
 * Spring Security espera autoridades con prefijo {@code ROLE_} para que funcione
 * {@code hasRole('PRODUCTOR')}. El nombre del principal es el claim {@code sub},
 * que es el identificador estable del usuario que guardan los servicios.
 */
public class KeycloakRolesConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    static final String CLAIM_REALM_ACCESS = "realm_access";
    static final String CLAIM_ROLES = "roles";
    static final String PREFIJO_ROL = "ROLE_";

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        return new JwtAuthenticationToken(jwt, extraerAutoridades(jwt), jwt.getSubject());
    }

    static Collection<GrantedAuthority> extraerAutoridades(Jwt jwt) {
        Object realmAccess = jwt.getClaims().get(CLAIM_REALM_ACCESS);
        if (!(realmAccess instanceof Map<?, ?> mapa)) {
            return List.of();
        }
        Object roles = mapa.get(CLAIM_ROLES);
        if (!(roles instanceof Collection<?> coleccion)) {
            return List.of();
        }
        return coleccion.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(rol -> (GrantedAuthority) new SimpleGrantedAuthority(PREFIJO_ROL + rol))
                .toList();
    }
}
