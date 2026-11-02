package ar.edu.tpi.commons.excepcion;

import org.springframework.http.HttpStatus;

/** 409: la operacion no es valida para el estado actual del recurso o duplica uno existente. */
public class ConflictoException extends ApiException {

    public ConflictoException(String codigo, String mensaje) {
        super(HttpStatus.CONFLICT, codigo, mensaje);
    }
}
