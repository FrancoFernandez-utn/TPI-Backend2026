package ar.edu.tpi.campos;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.tpi.campos.servicio.CamposService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** La migracion de datos de demostracion se aplica y deja el Lote Historico de productor1. */
@SpringBootTest(properties = "spring.flyway.locations=classpath:db/migration,classpath:db/demo")
class DatosDemoTest extends PostgresEmbebido {

    @Autowired
    private CamposService servicio;

    @Test
    void elLoteHistoricoPerteneceAProductor1() {
        var contexto = servicio.contexto(900001L);

        assertThat(contexto.titularSub()).isEqualTo("a1b2c3d4-0000-4000-8000-000000000002");
        assertThat(contexto.etapa()).hasToString("VEGETATIVO");
    }
}
