package ar.edu.tpi.misiones.servicio;

import ar.edu.tpi.misiones.dominio.ResultadoEvaluacion;
import java.time.Instant;

/** Datos de la evaluacion climatica que se guardan junto con la decision. */
public record DatosEvaluacion(Instant consultadaEn, String fuente, ResultadoEvaluacion resultado,
                              String variablesJson, String ventanasJson, String motivosJson) {
}
