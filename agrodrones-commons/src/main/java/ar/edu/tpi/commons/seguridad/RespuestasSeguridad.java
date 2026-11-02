package ar.edu.tpi.commons.seguridad;

import ar.edu.tpi.commons.error.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Respuestas 401 y 403 en el formato estandar de error.
 *
 * <p>Los errores de autenticacion ocurren en la cadena de filtros, antes de llegar
 * a los controladores, por eso no los atrapa el manejador global y necesitan su propio
 * punto de entrada.
 */
public final class RespuestasSeguridad {

    private RespuestasSeguridad() {
    }

    /** 401: falta el token o no es valido. */
    public static AuthenticationEntryPoint noAutenticado() {
        return (request, response, ex) -> escribir(request, response, HttpStatus.UNAUTHORIZED,
                "NO_AUTENTICADO", "Se requiere un token de acceso valido");
    }

    /** 403: el token es valido pero el rol no alcanza. */
    public static AccessDeniedHandler accesoDenegado() {
        return (request, response, ex) -> escribir(request, response, HttpStatus.FORBIDDEN,
                "ACCESO_DENEGADO", "No tiene permisos para esta operacion");
    }

    private static void escribir(HttpServletRequest request, HttpServletResponse response, HttpStatus status,
                                 String codigo, String mensaje) throws IOException {
        ErrorResponse error = new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), codigo,
                mensaje, request.getRequestURI(), List.of());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        if (status == HttpStatus.UNAUTHORIZED) {
            response.setHeader("WWW-Authenticate", "Bearer");
        }
        response.getWriter().write(error.toJson());
    }
}
