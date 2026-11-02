package ar.edu.tpi.misiones.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.tpi.commons.excepcion.ServicioNoDisponibleException;
import ar.edu.tpi.misiones.PostgresEmbebido;
import ar.edu.tpi.misiones.api.dto.SolicitudMisionRequest;
import ar.edu.tpi.misiones.cliente.CamposCliente;
import ar.edu.tpi.misiones.cliente.ContextoLote;
import ar.edu.tpi.misiones.dominio.Dron;
import ar.edu.tpi.misiones.dominio.EstadoMision;
import ar.edu.tpi.misiones.dominio.EtapaCultivo;
import ar.edu.tpi.misiones.dominio.ModeloDron;
import ar.edu.tpi.misiones.dominio.Piloto;
import ar.edu.tpi.misiones.dominio.Producto;
import ar.edu.tpi.misiones.dominio.TipoMision;
import ar.edu.tpi.misiones.repositorio.DronRepository;
import ar.edu.tpi.misiones.repositorio.MisionRepository;
import ar.edu.tpi.misiones.repositorio.ModeloDronRepository;
import ar.edu.tpi.misiones.repositorio.PilotoRepository;
import ar.edu.tpi.misiones.repositorio.ProductoRepository;
import ar.edu.tpi.misiones.servicio.SolicitudMisionService;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Proceso central de punta a punta dentro de ms-misiones: PostgreSQL real (embebido), proveedor climatico
 * simulado, reloj fijo y ms-campos reemplazado por un doble de prueba.
 *
 * <p>Reloj fijo: 11/03/2030 09:00 en Buenos Aires. El clima simulado BUENO tiene luz de 06:30 a 19:30.
 */
@SpringBootTest(properties = {
        "agrodrones.clima.proveedor=simulado",
        "agrodrones.clima.simulado.escenarios[0].lat=-33.0",
        "agrodrones.clima.simulado.escenarios[0].lon=-63.0",
        "agrodrones.clima.simulado.escenarios[0].escenario=VENTOSO",
        "agrodrones.clima.simulado.escenarios[1].lat=-34.0",
        "agrodrones.clima.simulado.escenarios[1].lon=-62.0",
        "agrodrones.clima.simulado.escenarios[1].escenario=SIN_SERVICIO"})
@AutoConfigureMockMvc
class SolicitudMisionIntegracionTest extends PostgresEmbebido {

    static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");
    static final LocalDate HOY = LocalDate.of(2030, 3, 11);
    static final Instant AHORA = HOY.atTime(9, 0).atZone(ZONA).toInstant();
    static final String PRODUCTOR = "productor-1";

    @TestConfiguration
    static class RelojFijo {
        @Bean
        @Primary
        Clock relojDePrueba() {
            return Clock.fixed(AHORA, ZONA);
        }
    }

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private ModeloDronRepository modelos;
    @Autowired
    private DronRepository drones;
    @Autowired
    private PilotoRepository pilotos;
    @Autowired
    private ProductoRepository productos;
    @Autowired
    private MisionRepository misiones;
    @Autowired
    private SolicitudMisionService solicitudes;

    @MockitoBean
    private CamposCliente campos;

    private ModeloDron pulverizador;
    private Producto herbicida;

    @BeforeEach
    void limpiarYPrepararCatalogo() {
        jdbc.execute("""
                DELETE FROM misiones.evaluacion_mision; DELETE FROM misiones.mision;
                DELETE FROM misiones.habilitacion; DELETE FROM misiones.piloto; DELETE FROM misiones.dron;
                DELETE FROM misiones.modelo_dron; DELETE FROM misiones.producto_etapa_prohibida;
                DELETE FROM misiones.producto""");
        // 10 ha/h y 40 ha por vuelo: un lote de 20 ha tarda 2 h exactas en un solo vuelo.
        pulverizador = modelos.save(new ModeloDron("Pulverizador", bd(40), bd(10), bd(40), bd(35), false));
        herbicida = productos.save(new Producto("Herbicida", 7, Set.of(EtapaCultivo.FLORACION)));
        lote(1L, PRODUCTOR, -31.42, -64.19, 20, EtapaCultivo.VEGETATIVO);
    }

    // ---------- Proceso completo ----------

    @Test
    void pulverizacionValidaQuedaProgramadaConVentanaPilotoYDron() throws Exception {
        Dron dron = dron(pulverizador, "LV-001");
        Piloto piloto = piloto("piloto-1", 30, TipoMision.PULVERIZACION);

        String cuerpo = solicitar(pulverizacion(1L, fin(HOY)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.estado").value("PROGRAMADA"))
                .andExpect(jsonPath("$.pilotoId").value(piloto.getId()))
                .andExpect(jsonPath("$.dronId").value(dron.getId()))
                .andExpect(jsonPath("$.vuelos").value(1))
                .andExpect(jsonPath("$.duracionEstimadaMin").value(120))
                .andExpect(jsonPath("$.ventana.inicio", notNullValue()))
                .andExpect(jsonPath("$.motivos", empty()))
                .andExpect(jsonPath("$.evaluacion.resultado").value("APTA"))
                .andExpect(jsonPath("$.evaluacion.fuente").value("simulado"))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(cuerpo, "$.id")).longValue();

        // La ventana elegida es de manana (mayor margen en el clima simulado) y dura exactamente 2 horas.
        Instant inicio = Instant.parse(JsonPath.read(cuerpo, "$.ventana.inicio"));
        Instant fin = Instant.parse(JsonPath.read(cuerpo, "$.ventana.fin"));
        assertThat(inicio.atZone(ZONA).toLocalTime()).isBetween(LocalTime.of(9, 0), LocalTime.of(11, 0));
        assertThat(java.time.Duration.between(inicio, fin).toMinutes()).isEqualTo(120);

        mvc.perform(get("/api/misiones/{id}/ventanas", id).with(usuario(PRODUCTOR, "PRODUCTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[0].modeloNombre").value("Pulverizador"));

        mvc.perform(get("/api/misiones/mias").with(usuario("piloto-1", "PILOTO")))
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/misiones/{id}", id).with(usuario("piloto-1", "PILOTO")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/misiones/{id}", id).with(usuario("otro-productor", "PRODUCTOR")))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/misiones/{id}", id).with(usuario("piloto-2", "PILOTO")))
                .andExpect(status().isNotFound());
    }

    @Test
    void monitoreoYSiembraUsanSusPropiasCondicionesDeEquipamiento() throws Exception {
        ModeloDron conCamara = modelos.save(new ModeloDron("Mavic Multi", bd(0), bd(20), bd(20), bd(38), true));
        dron(conCamara, "LV-CAM");
        piloto("piloto-multi", 5, TipoMision.MONITOREO, TipoMision.SIEMBRA_COBERTURA);

        solicitar(otraMision(1L, TipoMision.MONITOREO, fin(HOY)))
                .andExpect(jsonPath("$.estado").value("PROGRAMADA"));
        // SIEMBRA_COBERTURA no exige equipamiento (D-08): sirve el mismo dron sin tanque.
        solicitar(otraMision(1L, TipoMision.SIEMBRA_COBERTURA, fin(HOY)))
                .andExpect(jsonPath("$.estado").value("PROGRAMADA"));
    }

    // ---------- Reglas del lote y del producto ----------

    @Test
    void loteDeOtroProductorRespondeComoSiNoExistiera() throws Exception {
        lote(2L, "otro-productor", -31.4, -64.2, 20, EtapaCultivo.VEGETATIVO);

        solicitar(pulverizacion(2L, fin(HOY))).andExpect(status().isNotFound());
        assertThat(misiones.count()).isZero();
    }

    @Test
    void etapaProhibidaParaElProductoSeRechazaSinConsultarElClima() throws Exception {
        lote(3L, PRODUCTOR, -31.4, -64.2, 20, EtapaCultivo.FLORACION);
        dron(pulverizador, "LV-002");
        piloto("piloto-1", 30, TipoMision.PULVERIZACION);

        solicitar(pulverizacion(3L, fin(HOY)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("RECHAZADA"))
                .andExpect(jsonPath("$.motivos[0].codigo").value("ETAPA_PROHIBIDA"))
                .andExpect(jsonPath("$.evaluacion", nullValue()));
    }

    @Test
    void intervaloMinimoDesdeLaUltimaAplicacionCompletada() throws Exception {
        dron(pulverizador, "LV-003");
        piloto("piloto-1", 30, TipoMision.PULVERIZACION);
        // Historico de demostracion (D-02): aplicacion completada hace 2 dias; el intervalo es de 7.
        historicoCompletado(1L, herbicida, AHORA.minusSeconds(2 * 86_400));

        solicitar(pulverizacion(1L, fin(HOY.plusDays(2))))
                .andExpect(jsonPath("$.estado").value("RECHAZADA"))
                .andExpect(jsonPath("$.motivos[0].codigo").value("INTERVALO_MINIMO"));
    }

    @Test
    void elIntervaloSoloMiraMisionesCompletadasNoProgramadas() throws Exception {
        dron(pulverizador, "LV-004");
        piloto("piloto-1", 30, TipoMision.PULVERIZACION);

        solicitar(pulverizacion(1L, fin(HOY.plusDays(1)))).andExpect(jsonPath("$.estado").value("PROGRAMADA"));
        solicitar(pulverizacion(1L, fin(HOY.plusDays(1)))).andExpect(jsonPath("$.estado").value("PROGRAMADA"));
    }

    // ---------- Validacion de entrada y recursos inexistentes (D-16) ----------

    @Test
    void pulverizacionSinRecetaEsEntradaInvalida() throws Exception {
        solicitar("{\"loteId\":1,\"tipo\":\"PULVERIZACION\",\"productoId\":" + herbicida.getId()
                + ",\"fechaLimite\":\"" + fin(HOY) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles[0]", containsString("RN-08")));
    }

    @Test
    void monitoreoConProductoEsEntradaInvalida() throws Exception {
        solicitar("{\"loteId\":1,\"tipo\":\"MONITOREO\",\"productoId\":" + herbicida.getId()
                + ",\"fechaLimite\":\"" + fin(HOY) + "\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void fechaLimitePasadaEsEntradaInvalida() throws Exception {
        solicitar("{\"loteId\":1,\"tipo\":\"MONITOREO\",\"fechaLimite\":\"2020-01-01T00:00:00Z\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void productoInexistenteDevuelve404() throws Exception {
        solicitar("{\"loteId\":1,\"tipo\":\"PULVERIZACION\",\"productoId\":999999,\"recetaNro\":\"R-1\","
                + "\"fechaLimite\":\"" + fin(HOY) + "\"}")
                .andExpect(status().isNotFound());
    }

    // ---------- Clima ----------

    @Test
    void sinVentanaClimaticaSeInformaLaVariableQueMasBloqueo() throws Exception {
        lote(4L, PRODUCTOR, -33.0, -63.0, 20, EtapaCultivo.VEGETATIVO); // escenario VENTOSO
        dron(pulverizador, "LV-005");
        piloto("piloto-1", 30, TipoMision.PULVERIZACION);

        solicitar(pulverizacion(4L, fin(HOY.plusDays(2))))
                .andExpect(jsonPath("$.estado").value("RECHAZADA"))
                .andExpect(jsonPath("$.motivos[0].codigo").value("SIN_VENTANA_CLIMATICA"))
                .andExpect(jsonPath("$.motivos[0].detalle", containsString("franjas")))
                .andExpect(jsonPath("$.evaluacion.resultado").value("NO_APTA"));
    }

    @Test
    void siLaFuenteClimaticaNoRespondeFallaCerradoSinGuardarNada() throws Exception {
        lote(7L, PRODUCTOR, -34.0, -62.0, 20, EtapaCultivo.VEGETATIVO); // escenario SIN_SERVICIO
        dron(pulverizador, "LV-017");
        piloto("piloto-1", 30, TipoMision.PULVERIZACION);

        solicitar(pulverizacion(7L, fin(HOY)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().exists("Retry-After"));
        assertThat(misiones.count()).isZero();
    }

    @Test
    void siMsCamposNoRespondeFallaCerradoSinGuardarNada() throws Exception {
        when(campos.contexto(anyLong())).thenThrow(new ServicioNoDisponibleException("ms-campos", 30));

        solicitar(pulverizacion(1L, fin(HOY)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "30"))
                .andExpect(jsonPath("$.codigo").value("DEPENDENCIA_NO_DISPONIBLE"));
        assertThat(misiones.count()).isZero();
    }

    // ---------- Piloto y dron ----------

    @Test
    void sinDronAptoParaElTipoDeMision() throws Exception {
        dron(pulverizador, "LV-006"); // sin camara
        piloto("piloto-1", 30, TipoMision.MONITOREO);

        solicitar(otraMision(1L, TipoMision.MONITOREO, fin(HOY)))
                .andExpect(jsonPath("$.estado").value("RECHAZADA"))
                .andExpect(jsonPath("$.motivos[0].codigo").value("SIN_DRON_APTO"));
    }

    @Test
    void dronConSeguroVencidoElDiaDelVueloNoEsApto() throws Exception {
        drones.save(new Dron(pulverizador, "LV-007", HOY.plusYears(1), HOY.minusDays(1), AHORA));
        piloto("piloto-1", 30, TipoMision.PULVERIZACION);

        solicitar(pulverizacion(1L, fin(HOY)))
                .andExpect(jsonPath("$.motivos[0].codigo").value("SIN_DRON_APTO"));
    }

    @Test
    void pilotoSinHorasMinimasParaPulverizarNoSeAsigna() throws Exception {
        dron(pulverizador, "LV-008");
        piloto("piloto-novato", 19.9, TipoMision.PULVERIZACION);

        solicitar(pulverizacion(1L, fin(HOY)))
                .andExpect(jsonPath("$.motivos[0].codigo").value("SIN_PILOTO_DISPONIBLE"));
    }

    @Test
    void pilotoSinHabilitacionParaElTipoNoSeAsigna() throws Exception {
        dron(pulverizador, "LV-009");
        piloto("piloto-monitoreo", 100, TipoMision.MONITOREO);

        solicitar(pulverizacion(1L, fin(HOY)))
                .andExpect(jsonPath("$.motivos[0].codigo").value("SIN_PILOTO_DISPONIBLE"));
    }

    @Test
    void licenciaVencidaElDiaDelVueloAunqueHoyEsteVigente() throws Exception {
        dron(pulverizador, "LV-010");
        pilotos.save(new Piloto("piloto-vence", "L-1", HOY, bd(50), Set.of(TipoMision.PULVERIZACION), AHORA));
        // La ultima aplicacion (intervalo de 7 dias) habilita a aplicar recien desde manana a las 09:00,
        // asi que el vuelo cae manana: la licencia, vigente hoy, ya no lo esta ese dia (RN-01).
        historicoCompletado(1L, herbicida, AHORA.minusSeconds(6 * 86_400));

        solicitar(pulverizacion(1L, fin(HOY.plusDays(2))))
                .andExpect(jsonPath("$.estado").value("RECHAZADA"))
                .andExpect(jsonPath("$.motivos[0].codigo").value("SIN_PILOTO_DISPONIBLE"));
    }

    @Test
    void elDronNoSePuedeUsarEnDosMisionesSolapadas() throws Exception {
        dron(pulverizador, "LV-011");
        piloto("piloto-1", 30, TipoMision.PULVERIZACION);
        piloto("piloto-2", 30, TipoMision.PULVERIZACION);
        String hastaLasOnce = HOY.atTime(11, 0).atZone(ZONA).toInstant().toString(); // unica ventana: 9 a 11

        solicitar(pulverizacion(1L, hastaLasOnce)).andExpect(jsonPath("$.estado").value("PROGRAMADA"));
        solicitar(pulverizacion(1L, hastaLasOnce))
                .andExpect(jsonPath("$.estado").value("RECHAZADA"))
                .andExpect(jsonPath("$.motivos[0].codigo").value("SIN_DRON_DISPONIBLE"));
    }

    @Test
    void unPilotoNoPuedeSuperarSeisHorasProgramadasEnElDia() throws Exception {
        lote(5L, PRODUCTOR, -31.4, -64.2, 40, EtapaCultivo.VEGETATIVO); // 4 h por mision
        dron(pulverizador, "LV-012");
        dron(pulverizador, "LV-013");
        piloto("piloto-1", 30, TipoMision.PULVERIZACION);

        solicitar(pulverizacion(5L, fin(HOY))).andExpect(jsonPath("$.estado").value("PROGRAMADA"));
        // Hay otro dron libre y franjas libres, pero 4 h + 4 h superan el tope de 6 h del piloto.
        solicitar(pulverizacion(5L, fin(HOY)))
                .andExpect(jsonPath("$.estado").value("RECHAZADA"))
                .andExpect(jsonPath("$.motivos[0].codigo").value("SIN_PILOTO_DISPONIBLE"));
    }

    @Test
    void unaMisionDeMasDeSeisHorasNoEncuentraPiloto() throws Exception {
        lote(6L, PRODUCTOR, -31.4, -64.2, 70, EtapaCultivo.VEGETATIVO); // 7 h
        dron(pulverizador, "LV-014");
        piloto("piloto-1", 30, TipoMision.PULVERIZACION);

        solicitar(pulverizacion(6L, fin(HOY.plusDays(2))))
                .andExpect(jsonPath("$.motivos[0].codigo").value("SIN_PILOTO_DISPONIBLE"));
    }

    // ---------- Cancelacion (D-06) ----------

    @Test
    void cancelarLiberaElPilotoYElDron() throws Exception {
        dron(pulverizador, "LV-015");
        piloto("piloto-1", 30, TipoMision.PULVERIZACION);
        String hastaLasOnce = HOY.atTime(11, 0).atZone(ZONA).toInstant().toString();

        long id = idDe(solicitar(pulverizacion(1L, hastaLasOnce)).andExpect(jsonPath("$.estado").value("PROGRAMADA")));
        solicitar(pulverizacion(1L, hastaLasOnce)).andExpect(jsonPath("$.estado").value("RECHAZADA"));

        mvc.perform(post("/api/misiones/{id}/cancelar", id).with(usuario("otro-productor", "PRODUCTOR")))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/misiones/{id}/cancelar", id).with(usuario(PRODUCTOR, "PRODUCTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));
        mvc.perform(post("/api/misiones/{id}/cancelar", id).with(usuario(PRODUCTOR, "PRODUCTOR")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"));

        solicitar(pulverizacion(1L, hastaLasOnce)).andExpect(jsonPath("$.estado").value("PROGRAMADA"));
    }

    @Test
    void unaMisionRechazadaNoSePuedeCancelar() throws Exception {
        long id = idDe(solicitar(pulverizacion(1L, fin(HOY)))); // sin drones: rechazada
        mvc.perform(post("/api/misiones/{id}/cancelar", id).with(usuario(PRODUCTOR, "PRODUCTOR")))
                .andExpect(status().isConflict());
    }

    // ---------- Seguridad ----------

    @Test
    void soloElProductorPuedeSolicitar() throws Exception {
        mvc.perform(post("/api/misiones").with(usuario("piloto-1", "PILOTO"))
                        .contentType(MediaType.APPLICATION_JSON).content(pulverizacion(1L, fin(HOY))))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/misiones").contentType(MediaType.APPLICATION_JSON).content(pulverizacion(1L, fin(HOY))))
                .andExpect(status().isUnauthorized());
    }

    // ---------- Concurrencia (D-15) ----------

    @Test
    void dosSolicitudesSimultaneasPorElUnicoDronNoQuedanAmbasProgramadas() throws Exception {
        dron(pulverizador, "LV-016");
        piloto("piloto-1", 30, TipoMision.PULVERIZACION);
        piloto("piloto-2", 30, TipoMision.PULVERIZACION);
        Instant hastaLasOnce = HOY.atTime(11, 0).atZone(ZONA).toInstant();
        SolicitudMisionRequest req = new SolicitudMisionRequest(1L, TipoMision.PULVERIZACION, herbicida.getId(),
                "R-1", hastaLasOnce);

        CountDownLatch largada = new CountDownLatch(1);
        Callable<EstadoMision> tarea = () -> {
            largada.await();
            return solicitudes.solicitar(PRODUCTOR, req).getEstado();
        };
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<EstadoMision> a = pool.submit(tarea);
            Future<EstadoMision> b = pool.submit(tarea);
            largada.countDown();
            assertThat(List.of(a.get(), b.get()))
                    .containsExactlyInAnyOrder(EstadoMision.PROGRAMADA, EstadoMision.RECHAZADA);
        } finally {
            pool.shutdownNow();
        }
        assertThat(misiones.findAll()).filteredOn(m -> m.getEstado() == EstadoMision.PROGRAMADA).hasSize(1);
    }

    // ---------- Ayudas ----------

    private ResultActions solicitar(String cuerpo) throws Exception {
        return mvc.perform(post("/api/misiones").with(usuario(PRODUCTOR, "PRODUCTOR"))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private long idDe(ResultActions resultado) throws Exception {
        return ((Number) JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }

    private String pulverizacion(long loteId, String fechaLimite) {
        return "{\"loteId\":" + loteId + ",\"tipo\":\"PULVERIZACION\",\"productoId\":" + herbicida.getId()
                + ",\"recetaNro\":\"R-2030-001\",\"fechaLimite\":\"" + fechaLimite + "\"}";
    }

    private static String otraMision(long loteId, TipoMision tipo, String fechaLimite) {
        return "{\"loteId\":" + loteId + ",\"tipo\":\"" + tipo + "\",\"fechaLimite\":\"" + fechaLimite + "\"}";
    }

    /** Fin del dia de luz (19:30) de la fecha indicada. */
    private static String fin(LocalDate dia) {
        return dia.atTime(19, 30).atZone(ZONA).toInstant().toString();
    }

    private void lote(Long id, String titular, double lat, double lon, double hectareas, EtapaCultivo etapa) {
        when(campos.contexto(id)).thenReturn(new ContextoLote(id, titular, BigDecimal.valueOf(lat),
                BigDecimal.valueOf(lon), BigDecimal.valueOf(hectareas), "ACTIVO", "soja", etapa, AHORA));
    }

    private Dron dron(ModeloDron modelo, String matricula) {
        return drones.save(new Dron(modelo, matricula, HOY.plusYears(1), HOY.plusYears(1), AHORA));
    }

    private Piloto piloto(String sub, double horas, TipoMision... tipos) {
        return pilotos.save(new Piloto(sub, "L-" + sub, HOY.plusYears(1), BigDecimal.valueOf(horas),
                Set.of(tipos), AHORA));
    }

    private void historicoCompletado(Long loteId, Producto producto, Instant fin) {
        jdbc.update("""
                INSERT INTO misiones.mision (lote_id, productor_sub, tipo, producto_id, receta_nro, estado,
                    inicio_prev, fin_prev, fecha_limite, motivos_json, creada_en, actualizada_en)
                VALUES (?, ?, 'PULVERIZACION', ?, 'R-HIST', 'COMPLETADA', ?, ?, ?, '[]'::jsonb, ?, ?)""",
                loteId, PRODUCTOR, producto.getId(), ts(fin.minusSeconds(7200)), ts(fin), ts(fin), ts(fin), ts(fin));
    }

    private static java.sql.Timestamp ts(Instant i) {
        return java.sql.Timestamp.from(i);
    }

    private static BigDecimal bd(double v) {
        return BigDecimal.valueOf(v);
    }

    private static RequestPostProcessor usuario(String sub, String rol) {
        return jwt().jwt(j -> j.subject(sub)).authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }
}
