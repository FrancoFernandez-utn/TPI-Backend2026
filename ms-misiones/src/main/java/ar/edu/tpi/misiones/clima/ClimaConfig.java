package ar.edu.tpi.misiones.clima;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Elige el proveedor de pronostico segun agrodrones.clima.proveedor y configura la resiliencia. */
@Configuration
public class ClimaConfig {

    @Bean
    @ConditionalOnProperty(name = "agrodrones.clima.proveedor", havingValue = "openmeteo", matchIfMissing = true)
    ProveedorPronostico openMeteo(RestClient.Builder builder, CircuitBreakerFactory<?, ?> circuitos,
                                  ClimaProperties props) {
        HttpClient http = HttpClient.newBuilder().connectTimeout(props.openMeteo().timeoutConexion()).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(props.openMeteo().timeoutLectura());
        RestClient cliente = builder.clone().baseUrl(props.openMeteo().url()).requestFactory(fabrica).build();
        return new OpenMeteoProveedor(cliente, circuitos, props);
    }

    @Bean
    @ConditionalOnProperty(name = "agrodrones.clima.proveedor", havingValue = "simulado")
    ProveedorPronostico simulado(ClimaProperties props, Clock reloj) {
        return new SimuladoProveedor(props, reloj);
    }

    /**
     * Circuit breaker de Open-Meteo: si fallan la mitad de las ultimas 10 llamadas, deja de llamar
     * durante 30 s y responde 503 de inmediato. El limite de tiempo cubre los reintentos.
     */
    @Bean
    Customizer<Resilience4JCircuitBreakerFactory> circuitoOpenMeteo(ClimaProperties props) {
        Duration limite = props.openMeteo().timeoutConexion().plus(props.openMeteo().timeoutLectura())
                .multipliedBy(props.openMeteo().reintentos() + 1L)
                .plus(props.openMeteo().esperaReintento().multipliedBy(props.openMeteo().reintentos()));
        return fabrica -> fabrica.configure(b -> b
                .timeLimiterConfig(TimeLimiterConfig.custom().timeoutDuration(limite).build())
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .slidingWindowSize(10)
                        .minimumNumberOfCalls(5)
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofSeconds(30))
                        .build()), "open-meteo");
    }
}
