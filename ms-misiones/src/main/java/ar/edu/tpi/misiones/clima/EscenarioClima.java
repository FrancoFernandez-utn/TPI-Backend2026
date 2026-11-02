package ar.edu.tpi.misiones.clima;

/** Escenarios del proveedor simulado para la demostracion. */
public enum EscenarioClima {
    /** Mananas tranquilas y tardes algo mas ventosas; apto para todos los tipos de mision. */
    BUENO,
    /** Viento y rafagas fuertes todo el dia: no hay ventana para ningun tipo de mision. */
    VENTOSO,
    /** La fuente no responde: sirve para mostrar la falla cerrada (503). */
    SIN_SERVICIO
}
