package ar.edu.tpi.commons.excepcion;

import org.springframework.http.HttpStatus;

/**
 * 503: una dependencia necesaria no responde (otro microservicio o una API externa).
 * Se usa para fallar cerrado: sin la dependencia no se toma la decision.
 */
public class ServicioNoDisponibleException extends ApiException {

    private final long reintentarEnSegundos;

    public ServicioNoDisponibleException(String dependencia, long reintentarEnSegundos) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "DEPENDENCIA_NO_DISPONIBLE",
                dependencia + " no esta disponible; intente nuevamente mas tarde");
        this.reintentarEnSegundos = reintentarEnSegundos;
    }

    public long getReintentarEnSegundos() {
        return reintentarEnSegundos;
    }
}
