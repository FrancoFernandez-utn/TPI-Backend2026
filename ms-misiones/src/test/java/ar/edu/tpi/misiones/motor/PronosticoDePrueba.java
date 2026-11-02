package ar.edu.tpi.misiones.motor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/** Construye pronosticos sinteticos de varios dias, con valores buenos por defecto y excepciones puntuales. */
final class PronosticoDePrueba {

    static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");
    static final LocalDate DIA = LocalDate.of(2026, 11, 20);

    private PronosticoDePrueba() {
    }

    static Instant hora(int dia, int hora) {
        return DIA.plusDays(dia).atTime(LocalTime.of(hora, 0)).atZone(ZONA).toInstant();
    }

    /** Condiciones aptas para todos los tipos de mision. */
    static CondicionHora buena(Instant t) {
        return new CondicionHora(t, 20.0, 70.0, 0.0, 0.0, 8.0, 12.0, 270.0, 10.0);
    }

    static Pronostico dias(int cantidad, UnaryOperator<CondicionHora> ajuste) {
        List<CondicionHora> horas = new ArrayList<>();
        List<LuzSolar> luz = new ArrayList<>();
        for (int d = 0; d < cantidad; d++) {
            for (int h = 0; h < 24; h++) {
                horas.add(ajuste.apply(buena(hora(d, h))));
            }
            luz.add(new LuzSolar(DIA.plusDays(d), hora(d, 6).plusSeconds(30 * 60), hora(d, 19).plusSeconds(30 * 60)));
        }
        return new Pronostico(horas, luz);
    }

    static CondicionHora conViento(CondicionHora c, double viento) {
        return new CondicionHora(c.instante(), c.temperatura(), c.humedad(), c.precipitacion(),
                c.probabilidadPrecipitacion(), viento, c.rafaga(), c.direccion(), c.nubosidad());
    }

    static CondicionHora conRafaga(CondicionHora c, double rafaga) {
        return new CondicionHora(c.instante(), c.temperatura(), c.humedad(), c.precipitacion(),
                c.probabilidadPrecipitacion(), c.viento(), rafaga, c.direccion(), c.nubosidad());
    }

    static CondicionHora conLluvia(CondicionHora c, Double mm) {
        return new CondicionHora(c.instante(), c.temperatura(), c.humedad(), mm,
                c.probabilidadPrecipitacion(), c.viento(), c.rafaga(), c.direccion(), c.nubosidad());
    }

    static CondicionHora conTemperatura(CondicionHora c, Double temperatura) {
        return new CondicionHora(c.instante(), temperatura, c.humedad(), c.precipitacion(),
                c.probabilidadPrecipitacion(), c.viento(), c.rafaga(), c.direccion(), c.nubosidad());
    }
}
