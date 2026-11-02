package ar.edu.tpi.misiones.cliente;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Conexion con ms-campos (prefijo agrodrones.campos).
 *
 * @param url            URL interna de ms-campos (dentro de la red de Docker)
 * @param registracion   id de la registracion OAuth2 (client credentials) usada para obtener el token
 */
@ConfigurationProperties(prefix = "agrodrones.campos")
public record CamposProperties(
        @DefaultValue("http://localhost:8082") String url,
        @DefaultValue("ms-campos") String registracion,
        @DefaultValue("2s") Duration timeoutConexion,
        @DefaultValue("3s") Duration timeoutLectura,
        @DefaultValue("30") long reintentarEnSegundos) {
}
