package ar.edu.tpi.misiones.motor;

import ar.edu.tpi.misiones.dominio.VariableClima;

/**
 * Rango aceptable de una variable. Un extremo nulo significa "sin limite".
 * Ademas de decir si un valor cumple, calcula su margen: que tan lejos esta del limite mas cercano,
 * normalizado entre 0 (en el limite) y 1 (lo mas lejos posible). Sirve para elegir la mejor ventana (RN-15).
 */
public record RangoUmbral(VariableClima variable, Double minimo, boolean minInclusivo, Double maximo,
                          boolean maxInclusivo) {

    public boolean cumple(double valor) {
        if (minimo != null && (minInclusivo ? valor < minimo : valor <= minimo)) {
            return false;
        }
        return maximo == null || (maxInclusivo ? valor <= maximo : valor < maximo);
    }

    /** Margen normalizado del valor; null si el rango no permite medir distancia (por ejemplo, maximo 0). */
    public Double margen(double valor) {
        if (minimo != null && maximo != null) {
            double ancho = maximo - minimo;
            return ancho <= 0 ? null : acotar(Math.min(valor - minimo, maximo - valor) / (ancho / 2));
        }
        if (maximo != null) {
            return maximo <= 0 ? null : acotar((maximo - valor) / maximo);
        }
        if (minimo != null) {
            return minimo <= 0 ? null : acotar((valor - minimo) / minimo);
        }
        return null;
    }

    /** Devuelve un rango igual pero con un maximo estricto mas bajo si corresponde (limite de rafaga del dron). */
    public RangoUmbral conMaximoEstricto(double tope) {
        if (maximo != null && (maximo < tope || (maximo == tope && !maxInclusivo))) {
            return this;
        }
        return new RangoUmbral(variable, minimo, minInclusivo, tope, false);
    }

    private static double acotar(double v) {
        return Math.max(0, Math.min(1, v));
    }
}
