package ar.edu.tpi.misiones.motor;

import ar.edu.tpi.misiones.dominio.VariableClima;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Evalua cada franja horaria [h, h+1) del pronostico contra los umbrales de un tipo de mision
 * y el limite de rafaga del modelo de dron. Es logica pura: no usa Spring ni base de datos.
 *
 * <p>Como se lee cada variable en la franja [h, h+1), segun la semantica oficial de Open-Meteo (D-10):
 * <ul>
 *   <li>Instantaneas (viento, temperatura, humedad, nubosidad): deben cumplir en h y en h+1.</li>
 *   <li>Rafaga: maximo de la hora anterior a la marca, por eso se usa el valor de h+1.</li>
 *   <li>Precipitacion: suma de la hora anterior a la marca. "La hora y las N siguientes" son las
 *       marcas h+1 a h+1+N (por defecto N = 4).</li>
 * </ul>
 * Un valor faltante deja la franja no apta con causa SIN_DATOS (criterio conservador del dominio).
 */
public class EvaluadorFranjas {

    private static final long HORA = 3600;

    private final ZoneId zona;
    private final int horasLluviaSiguientes;

    public EvaluadorFranjas(ZoneId zona, int horasLluviaSiguientes) {
        this.zona = zona;
        this.horasLluviaSiguientes = horasLluviaSiguientes;
    }

    public List<EvaluacionFranja> evaluar(Pronostico pronostico, List<RangoUmbral> umbrales,
                                          double limiteRafagaModelo) {
        Map<VariableClima, RangoUmbral> rangos = rangosEfectivos(umbrales, limiteRafagaModelo);
        Map<Instant, CondicionHora> porInstante = new HashMap<>();
        pronostico.horas().forEach(h -> porInstante.put(h.instante(), h));

        List<EvaluacionFranja> resultado = new ArrayList<>();
        for (CondicionHora hora : pronostico.horas()) {
            resultado.add(evaluarFranja(hora.instante(), porInstante, rangos, pronostico.luz()));
        }
        return resultado;
    }

    private EvaluacionFranja evaluarFranja(Instant inicio, Map<Instant, CondicionHora> porInstante,
                                           Map<VariableClima, RangoUmbral> rangos, List<LuzSolar> luz) {
        Instant fin = inicio.plusSeconds(HORA);
        boolean diurna = esDiurna(inicio, fin, luz);
        CondicionHora enInicio = porInstante.get(inicio);
        CondicionHora enFin = porInstante.get(fin);

        Set<VariableClima> fallidas = EnumSet.noneOf(VariableClima.class);
        boolean sinDatos = false;
        double margen = 1.0;

        for (RangoUmbral rango : rangos.values()) {
            List<Double> valores = valoresParaFranja(rango.variable(), inicio, enInicio, enFin, porInstante);
            if (valores == null) {
                sinDatos = true;
                continue;
            }
            for (double valor : valores) {
                if (!rango.cumple(valor)) {
                    fallidas.add(rango.variable());
                }
                Double m = rango.margen(valor);
                if (m != null) {
                    margen = Math.min(margen, m);
                }
            }
        }

        double vientoMedio = promedio(enInicio, enFin, CondicionHora::viento);
        boolean apta = diurna && !sinDatos && fallidas.isEmpty();
        return new EvaluacionFranja(inicio, diurna, apta, fallidas, sinDatos, apta ? margen : 0.0, vientoMedio);
    }

    /** Valores que deben cumplir el umbral en la franja; null si falta alguno. */
    private List<Double> valoresParaFranja(VariableClima variable, Instant inicio, CondicionHora enInicio,
                                           CondicionHora enFin, Map<Instant, CondicionHora> porInstante) {
        return switch (variable) {
            case VIENTO -> instantaneos(enInicio, enFin, CondicionHora::viento);
            case TEMPERATURA -> instantaneos(enInicio, enFin, CondicionHora::temperatura);
            case HUMEDAD -> instantaneos(enInicio, enFin, CondicionHora::humedad);
            case NUBOSIDAD -> instantaneos(enInicio, enFin, CondicionHora::nubosidad);
            case RAFAGA -> enFin == null || enFin.rafaga() == null ? null : List.of(enFin.rafaga());
            case PRECIPITACION -> precipitacion(inicio, porInstante);
        };
    }

    private List<Double> instantaneos(CondicionHora a, CondicionHora b, Function<CondicionHora, Double> campo) {
        if (a == null || b == null || campo.apply(a) == null || campo.apply(b) == null) {
            return null;
        }
        return List.of(campo.apply(a), campo.apply(b));
    }

    private List<Double> precipitacion(Instant inicio, Map<Instant, CondicionHora> porInstante) {
        List<Double> valores = new ArrayList<>();
        for (int i = 1; i <= horasLluviaSiguientes + 1; i++) {
            CondicionHora h = porInstante.get(inicio.plusSeconds(HORA * i));
            if (h == null || h.precipitacion() == null) {
                return null;
            }
            valores.add(h.precipitacion());
        }
        return valores;
    }

    private boolean esDiurna(Instant inicio, Instant fin, List<LuzSolar> luz) {
        var fecha = inicio.atZone(zona).toLocalDate();
        Optional<LuzSolar> dia = luz.stream().filter(l -> l.fecha().equals(fecha)).findFirst();
        return dia.map(d -> d.contiene(inicio, fin)).orElse(false);
    }

    private static double promedio(CondicionHora a, CondicionHora b, Function<CondicionHora, Double> campo) {
        if (a == null || b == null || campo.apply(a) == null || campo.apply(b) == null) {
            return Double.MAX_VALUE;
        }
        return (campo.apply(a) + campo.apply(b)) / 2;
    }

    /** Umbrales del tipo de mision mas el limite de rafaga del modelo, que es estricto ("menor al limite"). */
    static Map<VariableClima, RangoUmbral> rangosEfectivos(List<RangoUmbral> umbrales, double limiteRafagaModelo) {
        Map<VariableClima, RangoUmbral> rangos = new EnumMap<>(VariableClima.class);
        umbrales.forEach(u -> rangos.put(u.variable(), u));
        RangoUmbral rafaga = rangos.get(VariableClima.RAFAGA);
        rangos.put(VariableClima.RAFAGA, rafaga == null
                ? new RangoUmbral(VariableClima.RAFAGA, null, false, limiteRafagaModelo, false)
                : rafaga.conMaximoEstricto(limiteRafagaModelo));
        return rangos;
    }
}
