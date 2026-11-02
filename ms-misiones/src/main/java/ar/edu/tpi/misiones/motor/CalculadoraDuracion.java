package ar.edu.tpi.misiones.motor;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * RN-14: duracion = superficie / rendimiento (ha por hora) + minutos de recambio por cada cambio de bateria.
 * Vuelos = superficie / ha por vuelo, redondeado hacia arriba. Hay un recambio entre cada par de vuelos
 * consecutivos, es decir vuelos - 1 recambios (decision A-14).
 */
public final class CalculadoraDuracion {

    private CalculadoraDuracion() {
    }

    public static Duracion calcular(BigDecimal hectareas, BigDecimal haPorHora, BigDecimal haPorVuelo,
                                    int minutosPorRecambio) {
        if (hectareas.signum() <= 0 || haPorHora.signum() <= 0 || haPorVuelo.signum() <= 0) {
            throw new IllegalArgumentException("Superficie y rendimientos deben ser positivos");
        }
        int vuelos = hectareas.divide(haPorVuelo, 0, RoundingMode.CEILING).intValueExact();
        int minutosDeVuelo = hectareas.multiply(BigDecimal.valueOf(60))
                .divide(haPorHora, MathContext.DECIMAL64)
                .setScale(0, RoundingMode.CEILING)
                .intValueExact();
        return new Duracion(vuelos, minutosDeVuelo + (vuelos - 1) * minutosPorRecambio);
    }
}
