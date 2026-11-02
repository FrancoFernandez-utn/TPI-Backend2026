package ar.edu.tpi.commons.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class KeycloakRolesConverterTest {

    private final KeycloakRolesConverter converter = new KeycloakRolesConverter();

    @Test
    void convierteRolesDelRealmEnAutoridadesConPrefijoYUsaElSubComoNombre() {
        Jwt jwt = jwt(Map.of("realm_access", Map.of("roles", List.of("PRODUCTOR", "offline_access"))));

        AbstractAuthenticationToken auth = converter.convert(jwt);

        assertThat(auth.getName()).isEqualTo("sub-123");
        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_PRODUCTOR", "ROLE_offline_access");
    }

    @Test
    void sinClaimRealmAccessNoAsignaAutoridades() {
        AbstractAuthenticationToken auth = converter.convert(jwt(Map.of("scope", "openid")));

        assertThat(auth.getAuthorities()).isEmpty();
    }

    @Test
    void ignoraRolesConFormatoInesperado() {
        Jwt jwt = jwt(Map.of("realm_access", Map.of("roles", "ADMIN")));

        assertThat(converter.convert(jwt).getAuthorities()).isEmpty();
    }

    private static Jwt jwt(Map<String, Object> claims) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("sub-123")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60));
        claims.forEach(builder::claim);
        return builder.build();
    }
}
