package ar.edu.tpi.commons.excepcion;

import org.springframework.http.HttpStatus;

/**
 * Excepcion base de la aplicacion: cada subclase define el codigo HTTP y un codigo funcional.
 * El manejador global la traduce al formato estandar de error.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;

    protected ApiException(HttpStatus status, String codigo, String mensaje) {
        super(mensaje);
        this.status = status;
        this.codigo = codigo;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCodigo() {
        return codigo;
    }
}
