package ar.edu.tpi.misiones.dominio;

/** Resultado de una evaluacion climatica (seccion 6 del dominio). */
public enum ResultadoEvaluacion {
    /** Hay al menos una ventana apta. */
    APTA,
    /** Hay datos pero ninguna ventana cumple. */
    NO_APTA,
    /** Ninguna franja tuvo datos completos. */
    SIN_DATOS
}
