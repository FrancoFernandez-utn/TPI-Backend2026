package ar.edu.tpi.commons.excepcion;

import org.springframework.http.HttpStatus;

/**
 * 404: el recurso no existe o pertenece a otro usuario.
 * Ante un recurso ajeno se usa la misma excepcion, para no revelar que existe.
 */
public class RecursoNoEncontradoException extends ApiException {

    public RecursoNoEncontradoException(String recurso, Object id) {
        super(HttpStatus.NOT_FOUND, "RECURSO_NO_ENCONTRADO", recurso + " " + id + " no encontrado");
    }
}
