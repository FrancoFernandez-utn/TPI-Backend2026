package ar.edu.tpi.commons.seguridad;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Acceso al usuario autenticado de la peticion en curso.
 *
 * <p>Los servicios lo usan para comparar el {@code sub} del token con el titular
 * de un recurso (por ejemplo, el productor dueno de un lote).
 */
public final class UsuarioActual {

    private UsuarioActual() {
    }

    /** Identificador del usuario ({@code sub} del JWT). */
    public static String sub() {
        return autenticacion().getName();
    }

    /** Indica si el usuario tiene el rol indicado (sin prefijo, por ejemplo "ADMIN"). */
    public static boolean tieneRol(String rol) {
        String autoridad = KeycloakRolesConverter.PREFIJO_ROL + rol;
        return autenticacion().getAuthorities().stream()
                .anyMatch(a -> autoridad.equals(a.getAuthority()));
    }

    private static Authentication autenticacion() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("No hay un usuario autenticado");
        }
        return auth;
    }
}
