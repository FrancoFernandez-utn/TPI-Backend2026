package ar.edu.tpi.misiones.dominio;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Estados de la mision en el MVP (D-18) y sus transiciones legales (seccion 6 del dominio).
 *
 * <p>COMPLETADA existe en el modelo porque RN-10 usa el historial de misiones completadas, pero en el
 * MVP solo aparece en datos historicos: no hay ninguna transicion que lleve a ese estado.
 */
public enum EstadoMision {
    SOLICITADA,
    AUTORIZADA,
    PROGRAMADA,
    RECHAZADA,
    CANCELADA,
    COMPLETADA;

    private static final Map<EstadoMision, Set<EstadoMision>> TRANSICIONES = Map.of(
            SOLICITADA, EnumSet.of(AUTORIZADA, RECHAZADA),
            AUTORIZADA, EnumSet.of(PROGRAMADA, CANCELADA),
            PROGRAMADA, EnumSet.of(CANCELADA),
            RECHAZADA, EnumSet.noneOf(EstadoMision.class),
            CANCELADA, EnumSet.noneOf(EstadoMision.class),
            COMPLETADA, EnumSet.noneOf(EstadoMision.class));

    /** Estados que reservan piloto y dron: los que cuentan para RN-03 y RN-06. */
    public static final Set<EstadoMision> RESERVAN_RECURSOS = EnumSet.of(AUTORIZADA, PROGRAMADA);

    public boolean puedePasarA(EstadoMision destino) {
        return TRANSICIONES.get(this).contains(destino);
    }
}
