package ar.edu.tpi.misiones.clima;

import ar.edu.tpi.commons.excepcion.ServicioNoDisponibleException;
import ar.edu.tpi.misiones.motor.CondicionHora;
import ar.edu.tpi.misiones.motor.LuzSolar;
import ar.edu.tpi.misiones.motor.Pronostico;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Cliente de Open-Meteo Forecast API. Unico punto del sistema que consulta el clima.
 *
 * <p>Resiliencia: timeout de conexion y de lectura (configurados en el RestClient), reintentos ante
 * errores transitorios y circuit breaker. Si despues de eso no hay respuesta, lanza
 * ServicioNoDisponibleException: la solicitud se rechaza con 503 y no se autoriza (falla cerrada).
 *
 * <p>Se piden las unidades en forma explicita (km/h, mm, grados Celsius) para no depender de los valores
 * por defecto, y los instantes como unixtime para evitar ambiguedades de zona horaria.
 */
public class OpenMeteoProveedor implements ProveedorPronostico {

    static final String VARIABLES_HORARIAS = String.join(",", "temperature_2m", "relative_humidity_2m",
            "precipitation", "precipitation_probability", "wind_speed_10m", "wind_gusts_10m",
            "wind_direction_10m", "cloud_cover");
    private static final String NOMBRE_CIRCUITO = "open-meteo";
    private static final long REINTENTAR_EN_SEGUNDOS = 60;
    private static final Logger log = LoggerFactory.getLogger(OpenMeteoProveedor.class);

    private final RestClient restClient;
    private final CircuitBreaker circuito;
    private final RetryTemplate reintentos;
    private final ClimaProperties props;
    private final ZoneId zona;

    public OpenMeteoProveedor(RestClient restClient, CircuitBreakerFactory<?, ?> circuitos, ClimaProperties props) {
        this.restClient = restClient;
        this.circuito = circuitos.create(NOMBRE_CIRCUITO);
        this.props = props;
        this.zona = ZoneId.of(props.zonaHoraria());
        this.reintentos = new RetryTemplate(RetryPolicy.builder()
                .maxRetries(props.openMeteo().reintentos())
                .delay(props.openMeteo().esperaReintento())
                // Un 4xx indica una peticion mal armada: reintentarla no cambia el resultado.
                .excludes(HttpClientErrorException.class)
                .build());
    }

    @Override
    public Pronostico obtener(double latitud, double longitud) {
        OpenMeteoRespuesta respuesta = circuito.run(() -> consultarConReintentos(latitud, longitud), error -> {
            log.warn("Open-Meteo no disponible para ({}, {}): {}", latitud, longitud, error.toString());
            throw new ServicioNoDisponibleException("Open-Meteo", REINTENTAR_EN_SEGUNDOS);
        });
        return convertir(respuesta, zona);
    }

    @Override
    public String nombre() {
        return "open-meteo";
    }

    private OpenMeteoRespuesta consultarConReintentos(double latitud, double longitud) {
        try {
            return reintentos.execute(() -> consultar(latitud, longitud));
        } catch (RetryException e) {
            throw new IllegalStateException("Open-Meteo no respondio", e.getCause());
        }
    }

    private OpenMeteoRespuesta consultar(double latitud, double longitud) {
        OpenMeteoRespuesta r = restClient.get()
                .uri(uri -> uri.path("/v1/forecast")
                        .queryParam("latitude", String.format(Locale.ROOT, "%.6f", latitud))
                        .queryParam("longitude", String.format(Locale.ROOT, "%.6f", longitud))
                        .queryParam("hourly", VARIABLES_HORARIAS)
                        .queryParam("daily", "sunrise,sunset")
                        .queryParam("timezone", props.zonaHoraria())
                        .queryParam("forecast_days", props.openMeteo().diasPronostico())
                        .queryParam("wind_speed_unit", "kmh")
                        .queryParam("temperature_unit", "celsius")
                        .queryParam("precipitation_unit", "mm")
                        .queryParam("timeformat", "unixtime")
                        .build())
                .retrieve()
                .body(OpenMeteoRespuesta.class);
        if (r == null || r.hourly() == null || r.hourly().time() == null) {
            throw new IllegalStateException("Respuesta de Open-Meteo sin serie horaria");
        }
        return r;
    }

    /**
     * Convierte la respuesta al modelo del motor. Un valor ausente o nulo queda como null en la hora:
     * el evaluador lo trata como SIN_DATOS (no apta), que es el criterio conservador del dominio.
     */
    static Pronostico convertir(OpenMeteoRespuesta r, ZoneId zona) {
        OpenMeteoRespuesta.Horario h = r.hourly();
        List<CondicionHora> horas = new ArrayList<>();
        for (int i = 0; i < h.time().size(); i++) {
            Long epoch = h.time().get(i);
            if (epoch == null) {
                continue;
            }
            horas.add(new CondicionHora(Instant.ofEpochSecond(epoch),
                    valor(h.temperatura(), i), valor(h.humedad(), i), valor(h.precipitacion(), i),
                    valor(h.probabilidadPrecipitacion(), i), valor(h.viento(), i), valor(h.rafaga(), i),
                    valor(h.direccion(), i), valor(h.nubosidad(), i)));
        }
        List<LuzSolar> luz = new ArrayList<>();
        OpenMeteoRespuesta.Diario d = r.daily();
        if (d != null && d.time() != null) {
            for (int i = 0; i < d.time().size(); i++) {
                Long dia = d.time().get(i);
                Long salida = valor(d.sunrise(), i);
                Long puesta = valor(d.sunset(), i);
                if (dia != null && salida != null && puesta != null) {
                    luz.add(new LuzSolar(Instant.ofEpochSecond(dia).atZone(zona).toLocalDate(),
                            Instant.ofEpochSecond(salida), Instant.ofEpochSecond(puesta)));
                }
            }
        }
        return new Pronostico(horas, luz);
    }

    private static <T> T valor(List<T> lista, int i) {
        return lista == null || i >= lista.size() ? null : lista.get(i);
    }
}
