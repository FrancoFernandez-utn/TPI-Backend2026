package ar.edu.tpi.misiones.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Valores de las reglas que el dominio presenta como configuracion (prefijo agrodrones.reglas).
 * Los valores por defecto son los ejemplos del documento de dominio.
 *
 * @param horasMinimasPulverizacion RN-02: horas de vuelo registradas que exige PULVERIZACION
 * @param maxHorasPilotoDia         RN-03: tope diario de horas programadas por piloto
 * @param horizonteHoras            RN-15: horizonte maximo de busqueda de ventanas
 * @param minutosRecambioBateria    RN-14: minutos por cada recambio de bateria
 * @param horasLluviaSiguientes     tabla 5.3: horas siguientes a la franja en las que se mira la lluvia
 * @param ventanasGuardadas         cuantas ventanas alternativas se guardan en la evaluacion
 */
@ConfigurationProperties(prefix = "agrodrones.reglas")
public record ReglasProperties(
        @DefaultValue("20") int horasMinimasPulverizacion,
        @DefaultValue("6") int maxHorasPilotoDia,
        @DefaultValue("72") int horizonteHoras,
        @DefaultValue("10") int minutosRecambioBateria,
        @DefaultValue("4") int horasLluviaSiguientes,
        @DefaultValue("20") int ventanasGuardadas) {
}
