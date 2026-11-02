package ar.edu.tpi.commons.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ErrorResponseTest {

    @Test
    void serializaAJsonEscapandoCaracteresEspeciales() {
        ErrorResponse error = new ErrorResponse(Instant.parse("2026-11-02T12:00:00Z"), 401, "Unauthorized",
                "NO_AUTENTICADO", "dice \"hola\"\n", "/api/x", List.of("a\\b"));

        assertThat(error.toJson()).isEqualTo(
                "{\"timestamp\":\"2026-11-02T12:00:00Z\",\"status\":401,\"error\":\"Unauthorized\","
                        + "\"codigo\":\"NO_AUTENTICADO\",\"mensaje\":\"dice \\\"hola\\\"\\n\",\"path\":\"/api/x\","
                        + "\"detalles\":[\"a\\\\b\"]}");
    }

    @Test
    void detallesNuloSeConvierteEnListaVacia() {
        ErrorResponse error = new ErrorResponse(Instant.now(), 404, "Not Found", "X", "m", "/p", null);

        assertThat(error.detalles()).isEmpty();
    }
}
