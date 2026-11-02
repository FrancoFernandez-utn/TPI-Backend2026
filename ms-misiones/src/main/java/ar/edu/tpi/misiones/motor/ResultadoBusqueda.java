package ar.edu.tpi.misiones.motor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Ventanas encontradas, ordenadas de mejor a peor, y cuantas franjas diurnas bloqueo cada causa
 * (una variable o SIN_DATOS). El conteo permite informar la causa principal cuando no hay ventana (RN-16).
 */
public record ResultadoBusqueda(List<Ventana> ventanas, Map<String, Integer> bloqueos) {

    public static final String SIN_DATOS = "SIN_DATOS";

    public ResultadoBusqueda {
        ventanas = List.copyOf(ventanas);
        bloqueos = Map.copyOf(bloqueos);
    }

    /** Causa que mas franjas bloqueo; en empate gana la primera en orden alfabetico, para que sea estable. */
    public Optional<String> causaPrincipal() {
        return bloqueos.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(Map.Entry::getKey)
                .findFirst();
    }
}
