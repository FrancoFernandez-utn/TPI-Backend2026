package ar.edu.tpi.commons.error;

import ar.edu.tpi.commons.excepcion.ApiException;
import ar.edu.tpi.commons.excepcion.ServicioNoDisponibleException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Traduce las excepciones al formato estandar {@link ErrorResponse}.
 *
 * <p>Separa las categorias de error definidas en el diseno (D-16):
 * entrada invalida (400), recurso inexistente o ajeno (404), conflicto de estado (409),
 * dependencia caida (503). Los rechazos por reglas de negocio no son errores HTTP:
 * se informan en la respuesta del recurso (por ejemplo, una mision RECHAZADA con motivos).
 */
@RestControllerAdvice
public class ManejadorGlobalErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorGlobalErrores.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> manejarApi(ApiException ex, HttpServletRequest req) {
        ResponseEntity.BodyBuilder respuesta = ResponseEntity.status(ex.getStatus());
        if (ex instanceof ServicioNoDisponibleException sin) {
            respuesta.header(HttpHeaders.RETRY_AFTER, String.valueOf(sin.getReintentarEnSegundos()));
            log.warn("Dependencia no disponible en {}: {}", req.getRequestURI(), ex.getMessage());
        }
        return respuesta.body(error(ex.getStatus(), ex.getCodigo(), ex.getMessage(), req, List.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarValidacion(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .sorted()
                .toList();
        List<String> globales = ex.getBindingResult().getGlobalErrors().stream()
                .map(ge -> ge.getDefaultMessage())
                .toList();
        List<String> todos = new java.util.ArrayList<>(detalles);
        todos.addAll(globales);
        return entrada("Los datos enviados no son validos", req, todos);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> manejarValidacionParametros(HandlerMethodValidationException ex,
                                                                    HttpServletRequest req) {
        List<String> detalles = ex.getAllErrors().stream()
                .map(e -> e.getDefaultMessage())
                .toList();
        return entrada("Los parametros enviados no son validos", req, detalles);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> manejarCuerpoIlegible(HttpMessageNotReadableException ex,
                                                              HttpServletRequest req) {
        return entrada("El cuerpo de la peticion no es un JSON valido o tiene valores con formato incorrecto",
                req, List.of());
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorResponse> manejarParametro(Exception ex, HttpServletRequest req) {
        return entrada("Parametro invalido o faltante", req, List.of(ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> manejarAccesoDenegado(AccessDeniedException ex, HttpServletRequest req) {
        HttpStatus status = HttpStatus.FORBIDDEN;
        return ResponseEntity.status(status)
                .body(error(status, "ACCESO_DENEGADO", "No tiene permisos para esta operacion", req, List.of()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> manejarRutaInexistente(NoResourceFoundException ex, HttpServletRequest req) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        return ResponseEntity.status(status)
                .body(error(status, "RUTA_NO_ENCONTRADA", "La ruta solicitada no existe", req, List.of()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> manejarMetodo(HttpRequestMethodNotSupportedException ex,
                                                      HttpServletRequest req) {
        HttpStatus status = HttpStatus.METHOD_NOT_ALLOWED;
        return ResponseEntity.status(status)
                .body(error(status, "METODO_NO_PERMITIDO", ex.getMessage(), req, List.of()));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> manejarTipoContenido(HttpMediaTypeNotSupportedException ex,
                                                             HttpServletRequest req) {
        HttpStatus status = HttpStatus.UNSUPPORTED_MEDIA_TYPE;
        return ResponseEntity.status(status)
                .body(error(status, "TIPO_DE_CONTENIDO_NO_SOPORTADO", ex.getMessage(), req, List.of()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejarInesperado(Exception ex, HttpServletRequest req) {
        log.error("Error inesperado en {}", req.getRequestURI(), ex);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status)
                .body(error(status, "ERROR_INTERNO", "Ocurrio un error inesperado", req, List.of()));
    }

    private ResponseEntity<ErrorResponse> entrada(String mensaje, HttpServletRequest req, List<String> detalles) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(error(status, "SOLICITUD_INVALIDA", mensaje, req, detalles));
    }

    private ErrorResponse error(HttpStatus status, String codigo, String mensaje, HttpServletRequest req,
                                List<String> detalles) {
        return new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), codigo, mensaje,
                req.getRequestURI(), detalles);
    }
}
