package ar.edu.tpi.misiones.api.dto;

import java.time.Instant;

/** Ventana apta encontrada para un modelo de dron, tal como se guarda en la evaluacion. */
public record VentanaPropuesta(Instant inicio, Instant fin, double margen, double vientoMedio, Long modeloId,
                               String modeloNombre) {
}
