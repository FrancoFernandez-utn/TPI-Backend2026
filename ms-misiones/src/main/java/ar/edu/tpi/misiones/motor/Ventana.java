package ar.edu.tpi.misiones.motor;

import java.time.Instant;

/** Intervalo continuo en el que la mision puede completarse. */
public record Ventana(Instant inicio, Instant fin, double margen, double vientoMedio) {
}
