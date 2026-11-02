package ar.edu.tpi.misiones.clima;

import ar.edu.tpi.commons.excepcion.ServicioNoDisponibleException;
import ar.edu.tpi.misiones.motor.CondicionHora;
import ar.edu.tpi.misiones.motor.LuzSolar;
import ar.edu.tpi.misiones.motor.Pronostico;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Proveedor simulado (D-14): genera un pronostico sintetico y determinista a partir del dia actual,
 * con la misma forma que el de Open-Meteo. Permite demostrar el sistema sin Internet y elegir el clima
 * de cada lote por sus coordenadas (configuracion agrodrones.clima.simulado.escenarios).
 *
 * <p>Como el pronostico se genera relativo a "hoy", la demo funciona cualquier dia sin fijar el reloj.
 */
public class SimuladoProveedor implements ProveedorPronostico {

    private static final double TOLERANCIA_COORDENADA = 0.0005;
    private static final LocalTime SALIDA_SOL = LocalTime.of(6, 30);
    private static final LocalTime PUESTA_SOL = LocalTime.of(19, 30);

    private final ClimaProperties props;
    private final Clock reloj;
    private final ZoneId zona;

    public SimuladoProveedor(ClimaProperties props, Clock reloj) {
        this.props = props;
        this.reloj = reloj;
        this.zona = ZoneId.of(props.zonaHoraria());
    }

    @Override
    public Pronostico obtener(double latitud, double longitud) {
        EscenarioClima escenario = escenarioPara(latitud, longitud);
        if (escenario == EscenarioClima.SIN_SERVICIO) {
            throw new ServicioNoDisponibleException("Proveedor climatico (simulado)", 60);
        }
        LocalDate hoy = LocalDate.now(reloj.withZone(zona));
        int dias = props.openMeteo().diasPronostico();
        List<CondicionHora> horas = new ArrayList<>();
        List<LuzSolar> luz = new ArrayList<>();
        for (int d = 0; d < dias; d++) {
            LocalDate fecha = hoy.plusDays(d);
            for (int h = 0; h < 24; h++) {
                Instant instante = fecha.atTime(h, 0).atZone(zona).toInstant();
                horas.add(escenario == EscenarioClima.VENTOSO ? ventoso(instante) : bueno(instante, h));
            }
            luz.add(new LuzSolar(fecha, fecha.atTime(SALIDA_SOL).atZone(zona).toInstant(),
                    fecha.atTime(PUESTA_SOL).atZone(zona).toInstant()));
        }
        return new Pronostico(horas, luz);
    }

    @Override
    public String nombre() {
        return "simulado";
    }

    EscenarioClima escenarioPara(double latitud, double longitud) {
        return props.simulado().escenarios().stream()
                .filter(e -> Math.abs(e.lat() - latitud) < TOLERANCIA_COORDENADA
                        && Math.abs(e.lon() - longitud) < TOLERANCIA_COORDENADA)
                .map(ClimaProperties.EscenarioPorUbicacion::escenario)
                .findFirst()
                .orElse(EscenarioClima.BUENO);
    }

    /** Manana tranquila, tarde algo mas ventosa y calida; siempre dentro de los umbrales del dominio. */
    private static CondicionHora bueno(Instant instante, int hora) {
        boolean tarde = hora >= 12 && hora <= 17;
        boolean manana = hora >= 7 && hora <= 11;
        double viento = tarde ? 12 : manana ? 7 : 5;
        double temperatura = tarde ? 26 : manana ? 20 : 15;
        double humedad = tarde ? 60 : manana ? 72 : 85;
        return new CondicionHora(instante, temperatura, humedad, 0.0, 5.0, viento, viento * 1.5, 270.0, 20.0);
    }

    private static CondicionHora ventoso(Instant instante) {
        return new CondicionHora(instante, 22.0, 55.0, 0.0, 5.0, 32.0, 48.0, 200.0, 30.0);
    }
}
