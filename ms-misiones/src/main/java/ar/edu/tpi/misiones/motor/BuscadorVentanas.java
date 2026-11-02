package ar.edu.tpi.misiones.motor;

import ar.edu.tpi.misiones.dominio.VariableClima;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RN-15: busca ventanas formadas por franjas aptas consecutivas, con luz solar, que alcancen para la
 * duracion estimada y que terminen antes del limite de busqueda. Es un recorrido simple sobre el arreglo
 * de franjas, sin Spring ni base de datos.
 *
 * <p>Orden de preferencia (A-15): mayor margen (la peor franja de la ventana), luego menor viento medio,
 * luego la mas temprana.
 */
public final class BuscadorVentanas {

    private static final Comparator<Ventana> MEJOR_PRIMERO = Comparator
            .comparingDouble(Ventana::margen).reversed()
            .thenComparingDouble(Ventana::vientoMedio)
            .thenComparing(Ventana::inicio);

    private BuscadorVentanas() {
    }

    /**
     * @param franjas  franjas evaluadas, ordenadas y consecutivas de a una hora
     * @param desde    inicio mas temprano permitido para la mision
     * @param hasta    fin mas tardio permitido (fecha limite o el horizonte de 72 h, lo que ocurra antes)
     * @param duracion duracion estimada de la mision
     */
    public static ResultadoBusqueda buscar(List<EvaluacionFranja> franjas, Instant desde, Instant hasta,
                                           Duracion duracion) {
        List<EvaluacionFranja> enRango = franjas.stream()
                .filter(f -> !f.inicio().isBefore(desde) && !f.fin().isAfter(hasta))
                .toList();

        int necesarias = duracion.franjasNecesarias();
        List<Ventana> ventanas = new ArrayList<>();
        for (int i = 0; i + necesarias <= enRango.size(); i++) {
            List<EvaluacionFranja> tramo = enRango.subList(i, i + necesarias);
            if (esContinuoYApto(tramo)) {
                Instant inicio = tramo.getFirst().inicio();
                double margen = tramo.stream().mapToDouble(EvaluacionFranja::margen).min().orElse(0);
                double viento = tramo.stream().mapToDouble(EvaluacionFranja::vientoMedio).average().orElse(0);
                ventanas.add(new Ventana(inicio, inicio.plusSeconds(duracion.minutos() * 60L), margen, viento));
            }
        }
        ventanas.sort(MEJOR_PRIMERO);
        return new ResultadoBusqueda(ventanas, contarBloqueos(enRango));
    }

    private static boolean esContinuoYApto(List<EvaluacionFranja> tramo) {
        for (int i = 0; i < tramo.size(); i++) {
            EvaluacionFranja f = tramo.get(i);
            if (!f.apta()) {
                return false;
            }
            if (i > 0 && !tramo.get(i - 1).fin().equals(f.inicio())) {
                return false;
            }
        }
        return true;
    }

    /** RN-16: cuenta, entre las franjas diurnas, cuantas veces cada causa impidio el vuelo. */
    private static Map<String, Integer> contarBloqueos(List<EvaluacionFranja> franjas) {
        Map<String, Integer> conteo = new HashMap<>();
        for (EvaluacionFranja f : franjas) {
            if (!f.diurna()) {
                continue;
            }
            for (VariableClima v : f.fallidas()) {
                conteo.merge(v.name(), 1, Integer::sum);
            }
            if (f.sinDatos()) {
                conteo.merge(ResultadoBusqueda.SIN_DATOS, 1, Integer::sum);
            }
        }
        return conteo;
    }
}
