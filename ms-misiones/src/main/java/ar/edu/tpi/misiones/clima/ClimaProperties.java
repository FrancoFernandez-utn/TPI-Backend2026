package ar.edu.tpi.misiones.clima;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuracion de la integracion climatica (prefijo agrodrones.clima).
 *
 * @param proveedor   "openmeteo" (real) o "simulado"
 * @param zonaHoraria zona usada para interpretar dias, salida y puesta del sol
 */
@ConfigurationProperties(prefix = "agrodrones.clima")
public record ClimaProperties(
        @DefaultValue("openmeteo") String proveedor,
        @DefaultValue("America/Argentina/Buenos_Aires") String zonaHoraria,
        @DefaultValue OpenMeteo openMeteo,
        @DefaultValue Simulado simulado) {

    /**
     * @param url               URL base de la API
     * @param diasPronostico    dias pedidos: 4 cubren 72 h mas las horas siguientes que mira la regla de lluvia
     * @param timeoutConexion   tiempo maximo para conectar
     * @param timeoutLectura    tiempo maximo para recibir la respuesta
     * @param reintentos        reintentos ante errores transitorios (timeout o 5xx)
     * @param esperaReintento   espera inicial entre reintentos
     */
    public record OpenMeteo(
            @DefaultValue("https://api.open-meteo.com") String url,
            @DefaultValue("4") int diasPronostico,
            @DefaultValue("3s") Duration timeoutConexion,
            @DefaultValue("5s") Duration timeoutLectura,
            @DefaultValue("2") int reintentos,
            @DefaultValue("500ms") Duration esperaReintento) {
    }

    /** Escenarios del proveedor simulado; un lote cuyas coordenadas no figuran usa BUENO. */
    public record Simulado(@DefaultValue List<EscenarioPorUbicacion> escenarios) {
    }

    public record EscenarioPorUbicacion(double lat, double lon, EscenarioClima escenario) {
    }
}
