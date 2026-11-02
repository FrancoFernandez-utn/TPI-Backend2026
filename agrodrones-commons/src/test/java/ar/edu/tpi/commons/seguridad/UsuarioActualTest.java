package ar.edu.tpi.commons.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class UsuarioActualTest {

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void devuelveElSubYLosRolesDelUsuarioAutenticado() {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                "sub-abc", null, List.of(new SimpleGrantedAuthority("ROLE_PILOTO"))));

        assertThat(UsuarioActual.sub()).isEqualTo("sub-abc");
        assertThat(UsuarioActual.tieneRol("PILOTO")).isTrue();
        assertThat(UsuarioActual.tieneRol("ADMIN")).isFalse();
    }

    @Test
    void sinAutenticacionLanzaExcepcion() {
        assertThatThrownBy(UsuarioActual::sub).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }
}
