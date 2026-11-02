package ar.edu.tpi.misiones.clima;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Respuesta de Open-Meteo pedida con timeformat=unixtime (instantes en segundos desde 1970). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenMeteoRespuesta(Horario hourly, Diario daily) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Horario(
            List<Long> time,
            @JsonProperty("temperature_2m") List<Double> temperatura,
            @JsonProperty("relative_humidity_2m") List<Double> humedad,
            @JsonProperty("precipitation") List<Double> precipitacion,
            @JsonProperty("precipitation_probability") List<Double> probabilidadPrecipitacion,
            @JsonProperty("wind_speed_10m") List<Double> viento,
            @JsonProperty("wind_gusts_10m") List<Double> rafaga,
            @JsonProperty("wind_direction_10m") List<Double> direccion,
            @JsonProperty("cloud_cover") List<Double> nubosidad) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Diario(List<Long> time, List<Long> sunrise, List<Long> sunset) {
    }
}
