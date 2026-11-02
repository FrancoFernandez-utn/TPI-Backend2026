package ar.edu.tpi.misiones.config;

import ar.edu.tpi.misiones.clima.ClimaProperties;
import ar.edu.tpi.misiones.motor.EvaluadorFranjas;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** El motor es logica pura; aca solo se le pasan sus parametros desde la configuracion. */
@Configuration
public class MotorConfig {

    @Bean
    EvaluadorFranjas evaluadorFranjas(ClimaProperties clima, ReglasProperties reglas) {
        return new EvaluadorFranjas(ZoneId.of(clima.zonaHoraria()), reglas.horasLluviaSiguientes());
    }
}
