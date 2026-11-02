package ar.edu.tpi.misiones.clima;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import ar.edu.tpi.commons.excepcion.ServicioNoDisponibleException;
import ar.edu.tpi.misiones.motor.CondicionHora;
import ar.edu.tpi.misiones.motor.Pronostico;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenMeteoProveedorTest {

    /** 2026-11-20 00:00 en Buenos Aires (UTC-3). */
    private static final long MEDIANOCHE = java.time.LocalDate.of(2026, 11, 20)
            .atStartOfDay(java.time.ZoneId.of("America/Argentina/Buenos_Aires")).toEpochSecond();

    private static final String RESPUESTA = """
            {"latitude":-31.42,"longitude":-64.19,"utc_offset_seconds":-10800,
             "hourly":{
               "time":[%d,%d,%d],
               "temperature_2m":[18.5,19.0,null],
               "relative_humidity_2m":[80,78,75],
               "precipitation":[0.0,0.0,0.4],
               "precipitation_probability":[5,5,40],
               "wind_speed_10m":[6.1,7.2,8.0],
               "wind_gusts_10m":[10.0,12.5,15.0],
               "wind_direction_10m":[270,265,260],
               "cloud_cover":[10,20,30]},
             "daily":{"time":[%d],"sunrise":[%d],"sunset":[%d]}}
            """.formatted(MEDIANOCHE, MEDIANOCHE + 3600, MEDIANOCHE + 7200,
            MEDIANOCHE, MEDIANOCHE + 6 * 3600 + 1800, MEDIANOCHE + 19 * 3600 + 1800);

    private MockRestServiceServer servidor;
    private OpenMeteoProveedor proveedor;

    @BeforeEach
    void configurar() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.open-meteo.com");
        servidor = MockRestServiceServer.bindTo(builder).build();
        ClimaProperties props = new ClimaProperties("openmeteo", "America/Argentina/Buenos_Aires",
                new ClimaProperties.OpenMeteo("https://api.open-meteo.com", 4, Duration.ofSeconds(1),
                        Duration.ofSeconds(1), 2, Duration.ofMillis(5)),
                new ClimaProperties.Simulado(List.of()));
        Resilience4JCircuitBreakerFactory circuitos = new Resilience4JCircuitBreakerFactory(
                CircuitBreakerRegistry.ofDefaults(),
                TimeLimiterRegistry.of(TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(5)).build()),
                null);
        proveedor = new OpenMeteoProveedor(builder.build(), circuitos, props);
    }

    @Test
    void pideLasVariablesYUnidadesExplicitasYConvierteLaRespuesta() {
        servidor.expect(once(), requestTo(Matchers.startsWith("https://api.open-meteo.com/v1/forecast")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("hourly", OpenMeteoProveedor.VARIABLES_HORARIAS))
                .andExpect(queryParam("daily", "sunrise,sunset"))
                .andExpect(queryParam("wind_speed_unit", "kmh"))
                .andExpect(queryParam("timeformat", "unixtime"))
                .andExpect(queryParam("timezone", "America/Argentina/Buenos_Aires"))
                .andExpect(queryParam("forecast_days", "4"))
                .andRespond(withSuccess(RESPUESTA, MediaType.APPLICATION_JSON));

        Pronostico p = proveedor.obtener(-31.42, -64.19);

        servidor.verify();
        assertThat(p.horas()).hasSize(3);
        CondicionHora primera = p.horas().getFirst();
        assertThat(primera.instante()).isEqualTo(Instant.ofEpochSecond(MEDIANOCHE));
        assertThat(primera.viento()).isEqualTo(6.1);
        assertThat(primera.rafaga()).isEqualTo(10.0);
        assertThat(primera.direccion()).isEqualTo(270.0);
        assertThat(p.luz()).hasSize(1);
        assertThat(p.luz().getFirst().fecha()).isEqualTo(LocalDate.of(2026, 11, 20));
    }

    @Test
    void conservaLosValoresNulosParaQueElMotorLosTrateComoSinDatos() {
        servidor.expect(once(), requestTo(Matchers.startsWith("https://api.open-meteo.com/v1/forecast")))
                .andRespond(withSuccess(RESPUESTA, MediaType.APPLICATION_JSON));

        Pronostico p = proveedor.obtener(-31.42, -64.19);

        assertThat(p.horas().get(2).temperatura()).isNull();
    }

    @Test
    void reintentaErroresTransitoriosYLuegoResponde() {
        servidor.expect(times(2), requestTo(Matchers.startsWith("https://api.open-meteo.com/v1/forecast")))
                .andRespond(withServerError());
        servidor.expect(once(), requestTo(Matchers.startsWith("https://api.open-meteo.com/v1/forecast")))
                .andRespond(withSuccess(RESPUESTA, MediaType.APPLICATION_JSON));

        Pronostico p = proveedor.obtener(-31.42, -64.19);

        servidor.verify();
        assertThat(p.horas()).hasSize(3);
    }

    @Test
    void siSigueFallandoDespuesDeLosReintentosFallaCerradoCon503() {
        servidor.expect(times(3), requestTo(Matchers.startsWith("https://api.open-meteo.com/v1/forecast")))
                .andRespond(withServerError());

        assertThatThrownBy(() -> proveedor.obtener(-31.42, -64.19))
                .isInstanceOf(ServicioNoDisponibleException.class)
                .hasMessageContaining("Open-Meteo");
        servidor.verify();
    }

    @Test
    void unErrorDePeticionNoSeReintenta() {
        servidor.expect(once(), requestTo(Matchers.startsWith("https://api.open-meteo.com/v1/forecast")))
                .andRespond(withBadRequest());

        assertThatThrownBy(() -> proveedor.obtener(-31.42, -64.19))
                .isInstanceOf(ServicioNoDisponibleException.class);
        servidor.verify();
    }

    @Test
    void unaRespuestaSinSerieHorariaSeTrataComoFalla() {
        servidor.expect(times(3), requestTo(Matchers.startsWith("https://api.open-meteo.com/v1/forecast")))
                .andRespond(withSuccess("{\"latitude\":-31.4}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> proveedor.obtener(-31.42, -64.19))
                .isInstanceOf(ServicioNoDisponibleException.class);
    }
}
