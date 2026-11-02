package ar.edu.tpi.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;
import java.time.Instant;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Prueba el Gateway con dos backends falsos (uno por microservicio) para verificar el enrutamiento,
 * la validacion del token y el bloqueo de las rutas internas.
 */
@SpringBootTest
class GatewayIntegracionTest {

    private static final Backend CAMPOS = new Backend("ms-campos");
    private static final Backend MISIONES = new Backend("ms-misiones");

    @DynamicPropertySource
    static void backends(DynamicPropertyRegistry registro) {
        registro.add("MS_CAMPOS_URL", CAMPOS::url);
        registro.add("MS_MISIONES_URL", MISIONES::url);
    }

    @AfterAll
    static void detener() {
        CAMPOS.detener();
        MISIONES.detener();
    }

    @Autowired
    private ApplicationContext contexto;

    private WebTestClient cliente;

    /** Reemplaza la validacion contra Keycloak: solo "token-del-usuario" es valido. */
    @MockitoBean
    private ReactiveJwtDecoder decodificador;

    @BeforeEach
    void limpiar() {
        when(decodificador.decode(anyString())).thenReturn(Mono.error(new BadJwtException("token invalido")));
        when(decodificador.decode("token-del-usuario")).thenReturn(Mono.just(Jwt.withTokenValue("token-del-usuario")
                .header("alg", "RS256").subject("productor-1").issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300)).build()));
        cliente = WebTestClient.bindToApplicationContext(contexto).apply(springSecurity()).configureClient().build();
        CAMPOS.recibidas.clear();
        MISIONES.recibidas.clear();
    }

    @Test
    void sinTokenDevuelve401ConElFormatoComunYNoLlegaAlServicio() {
        cliente.get().uri("/api/lotes/mios").exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueEquals(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .expectBody().jsonPath("$.codigo").isEqualTo("NO_AUTENTICADO");
        assertThat(CAMPOS.recibidas).isEmpty();
    }

    @Test
    void tokenMalFormadoDevuelve401() {
        cliente.get().uri("/api/misiones/mias").header(HttpHeaders.AUTHORIZATION, "Bearer no-es-un-jwt")
                .exchange()
                .expectStatus().isUnauthorized();
        assertThat(MISIONES.recibidas).isEmpty();
    }

    @Test
    void enrutaCadaRecursoAlServicioQueEsDuenio() {
        cliente.mutateWith(mockJwt()).get().uri("/api/lotes/mios").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.servicio").isEqualTo("ms-campos");
        cliente.mutateWith(mockJwt()).post().uri("/api/misiones").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.servicio").isEqualTo("ms-misiones");
        cliente.mutateWith(mockJwt()).get().uri("/api/umbrales").exchange()
                .expectBody().jsonPath("$.servicio").isEqualTo("ms-misiones");

        assertThat(CAMPOS.recibidas).containsExactly("GET /api/lotes/mios");
        assertThat(MISIONES.recibidas).containsExactly("POST /api/misiones", "GET /api/umbrales");
    }

    @Test
    void reenviaElTokenDelUsuarioAlServicio() {
        cliente.get().uri("/api/establecimientos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer token-del-usuario")
                .exchange()
                .expectStatus().isOk();
        assertThat(CAMPOS.ultimaAutorizacion).isEqualTo("Bearer token-del-usuario");
    }

    @Test
    void lasRutasInternasNoSeExponenNiConUnTokenValido() {
        cliente.mutateWith(mockJwt()).get().uri("/internal/lotes/1/contexto").exchange()
                .expectStatus().isForbidden()
                .expectBody().jsonPath("$.codigo").isEqualTo("ACCESO_DENEGADO");
        assertThat(CAMPOS.recibidas).isEmpty();
    }

    @Test
    void ladocumentacionEsPublicaYSeReescribeHaciaElServicio() {
        cliente.get().uri("/docs/campos/v3/api-docs").exchange().expectStatus().isOk();
        cliente.get().uri("/docs/misiones/v3/api-docs").exchange().expectStatus().isOk();

        assertThat(CAMPOS.recibidas).containsExactly("GET /v3/api-docs");
        assertThat(MISIONES.recibidas).containsExactly("GET /v3/api-docs");
    }

    @Test
    void cualquierOtraRutaSeRechaza() {
        cliente.mutateWith(mockJwt()).get().uri("/otra/cosa").exchange().expectStatus().isForbidden();
    }

    /** Backend HTTP minimo que responde quien es y registra lo que recibio. */
    static final class Backend {
        final List<String> recibidas = new CopyOnWriteArrayList<>();
        volatile String ultimaAutorizacion;
        private final HttpServer servidor;

        Backend(String nombre) {
            try {
                servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            servidor.createContext("/", intercambio -> {
                recibidas.add(intercambio.getRequestMethod() + " " + intercambio.getRequestURI().getPath());
                ultimaAutorizacion = intercambio.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                byte[] cuerpo = ("{\"servicio\":\"" + nombre + "\"}").getBytes(StandardCharsets.UTF_8);
                intercambio.getResponseHeaders().add("Content-Type", "application/json");
                intercambio.sendResponseHeaders(200, cuerpo.length);
                try (OutputStream os = intercambio.getResponseBody()) {
                    os.write(cuerpo);
                }
            });
            servidor.start();
        }

        String url() {
            return "http://127.0.0.1:" + servidor.getAddress().getPort();
        }

        void detener() {
            servidor.stop(0);
        }
    }
}
