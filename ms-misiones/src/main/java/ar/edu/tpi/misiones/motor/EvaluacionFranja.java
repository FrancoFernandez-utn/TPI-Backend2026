package ar.edu.tpi.misiones.motor;

import ar.edu.tpi.misiones.dominio.VariableClima;
import java.time.Instant;
import java.util.Set;

/**
 * Resultado de evaluar una franja horaria [inicio, inicio + 1 h).
 *
 * @param diurna     la franja cae completa entre la salida y la puesta del sol
 * @param apta       diurna, con datos completos y todas las variables dentro de los umbrales
 * @param fallidas   variables fuera de umbral
 * @param sinDatos   falto algun valor necesario (criterio conservador: no apta)
 * @param margen     peor margen de las variables evaluadas (0 a 1)
 * @param vientoMedio viento medio de la franja, para desempatar ventanas
 */
public record EvaluacionFranja(
        Instant inicio,
        boolean diurna,
        boolean apta,
        Set<VariableClima> fallidas,
        boolean sinDatos,
        double margen,
        double vientoMedio) {

    public EvaluacionFranja {
        fallidas = Set.copyOf(fallidas);
    }

    public Instant fin() {
        return inicio.plusSeconds(3600);
    }
}
