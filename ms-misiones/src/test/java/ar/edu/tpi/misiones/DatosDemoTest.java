package ar.edu.tpi.misiones;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.tpi.misiones.repositorio.MisionRepository;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** La mision COMPLETADA historica (D-02) queda disponible para RN-10. */
@SpringBootTest(properties = "spring.flyway.locations=classpath:db/migration,classpath:db/demo")
class DatosDemoTest extends PostgresEmbebido {

    @Autowired
    private MisionRepository misiones;

    @Test
    void laAplicacionHistoricaTerminoHaceDosDias() {
        Instant ultima = misiones.ultimaAplicacionCompletada(900001L, 900001L);

        assertThat(ultima).isNotNull();
        assertThat(Duration.between(ultima, Instant.now()).toHours()).isBetween(47L, 49L);
    }
}
