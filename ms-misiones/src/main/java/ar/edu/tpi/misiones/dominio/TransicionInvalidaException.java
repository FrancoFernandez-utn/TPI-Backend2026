package ar.edu.tpi.misiones.dominio;

import ar.edu.tpi.commons.excepcion.ConflictoException;

/** 409: se intento un cambio de estado que la maquina de estados no permite. */
public class TransicionInvalidaException extends ConflictoException {

    public TransicionInvalidaException(EstadoMision desde, EstadoMision hacia) {
        super("TRANSICION_INVALIDA", "Una mision " + desde + " no puede pasar a " + hacia);
    }
}
