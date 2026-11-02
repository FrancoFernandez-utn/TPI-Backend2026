package ar.edu.tpi.misiones.servicio;

import ar.edu.tpi.misiones.dominio.ModeloDron;
import ar.edu.tpi.misiones.motor.Duracion;
import ar.edu.tpi.misiones.motor.Ventana;

/** Ventana climatica valida para un modelo de dron concreto (la duracion depende del modelo). */
public record VentanaCandidata(Ventana ventana, ModeloDron modelo, Duracion duracion) {
}
