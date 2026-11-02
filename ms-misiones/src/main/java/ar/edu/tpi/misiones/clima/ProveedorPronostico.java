package ar.edu.tpi.misiones.clima;

import ar.edu.tpi.misiones.motor.Pronostico;

/**
 * Fuente del pronostico horario. Hay dos implementaciones, elegidas por configuracion:
 * Open-Meteo (real) y un proveedor simulado para pruebas y demostraciones sin Internet.
 *
 * <p>Si la fuente no responde, la implementacion lanza ServicioNoDisponibleException:
 * sin pronostico no se autoriza ninguna mision (falla cerrada).
 */
public interface ProveedorPronostico {

    Pronostico obtener(double latitud, double longitud);

    /** Nombre de la fuente, para dejar constancia en la evaluacion. */
    String nombre();
}
