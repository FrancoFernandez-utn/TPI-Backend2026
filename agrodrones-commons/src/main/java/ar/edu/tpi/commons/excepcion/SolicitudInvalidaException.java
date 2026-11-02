package ar.edu.tpi.commons.excepcion;

import org.springframework.http.HttpStatus;

/**
 * 400: la entrada tiene un formato o una combinacion de datos invalida
 * que no se puede expresar con anotaciones de Bean Validation.
 */
public class SolicitudInvalidaException extends ApiException {

    public SolicitudInvalidaException(String mensaje) {
        super(HttpStatus.BAD_REQUEST, "SOLICITUD_INVALIDA", mensaje);
    }
}
