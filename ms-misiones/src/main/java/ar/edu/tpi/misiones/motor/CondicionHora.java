package ar.edu.tpi.misiones.motor;

import java.time.Instant;

/**
 * Valores del pronostico para una marca horaria. Cualquier valor puede ser nulo si la API no lo informo.
 *
 * <p>Semantica temporal (documentacion oficial de Open-Meteo): la precipitacion y la rafaga de la marca T
 * corresponden a la hora anterior a T (suma y maximo); el resto de las variables son instantaneas en T.
 *
 * @param viento     velocidad media del viento a 10 m, km/h
 * @param rafaga     rafaga maxima a 10 m, km/h
 * @param direccion  direccion desde la que sopla el viento, grados
 */
public record CondicionHora(
        Instant instante,
        Double temperatura,
        Double humedad,
        Double precipitacion,
        Double probabilidadPrecipitacion,
        Double viento,
        Double rafaga,
        Double direccion,
        Double nubosidad) {
}
