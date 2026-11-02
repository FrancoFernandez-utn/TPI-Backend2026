package ar.edu.tpi.misiones.motor;

import java.time.Instant;
import java.time.LocalDate;

/** Salida y puesta del sol de un dia local. */
public record LuzSolar(LocalDate fecha, Instant salida, Instant puesta) {

    public boolean contiene(Instant inicio, Instant fin) {
        return !inicio.isBefore(salida) && !fin.isAfter(puesta);
    }
}
