package ar.edu.tpi.misiones.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Reloj inyectable: permite fijar la hora en las pruebas. */
@Configuration
public class RelojConfig {

    @Bean
    Clock reloj() {
        return Clock.systemUTC();
    }
}
