package ar.edu.tpi.misiones;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base de las pruebas de integracion: levanta un PostgreSQL 16 real dentro del proceso
 * (una sola vez para toda la ejecucion) y apunta el datasource a el. Las migraciones de
 * Flyway corren igual que en produccion.
 */
public abstract class PostgresEmbebido {

    private static final EmbeddedPostgres POSTGRES = iniciar();

    private static EmbeddedPostgres iniciar() {
        try {
            return EmbeddedPostgres.builder().start();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo iniciar PostgreSQL embebido", e);
        }
    }

    @DynamicPropertySource
    static void propiedadesBaseDeDatos(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registro.add("spring.datasource.username", () -> "postgres");
        registro.add("spring.datasource.password", () -> "postgres");
        registro.add("spring.flyway.create-schemas", () -> "true");
    }
}
