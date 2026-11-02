package ar.edu.tpi.campos.api;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.tpi.campos.PostgresEmbebido;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
class CamposApiIntegracionTest extends PostgresEmbebido {

    private static final String LOTE_JSON = """
            {"nombre":"Lote Norte","lat":-31.4201,"lon":-64.1888,"hectareas":40,
             "cultivo":"soja","etapa":"VEGETATIVO"}""";

    @Autowired
    private MockMvc mvc;

    @Test
    void productorCreaEstablecimientoYLoteYLoVeEnSusLotes() throws Exception {
        String productor = "productor-flujo";
        long establecimientoId = crearEstablecimiento(productor, "La Esperanza");

        mvc.perform(post("/api/establecimientos/{id}/lotes", establecimientoId).with(rol(productor, "PRODUCTOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(LOTE_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.hectareas").value(40))
                .andExpect(jsonPath("$.cultivoActual.etapa").value("VEGETATIVO"));

        mvc.perform(get("/api/lotes/mios").with(rol(productor, "PRODUCTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].establecimientoNombre").value("La Esperanza"));
    }

    @Test
    void cambioDeCultivoAgregaHistorialYElContextoInformaLaEtapaMasReciente() throws Exception {
        String productor = "productor-etapas";
        long loteId = crearLote(productor);

        mvc.perform(put("/api/lotes/{id}/cultivo", loteId).with(rol(productor, "PRODUCTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cultivo\":\"soja\",\"etapa\":\"FLORACION\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cultivoActual.etapa").value("FLORACION"));

        mvc.perform(get("/internal/lotes/{id}/contexto", loteId).with(rol("ms-misiones", "SERVICIO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loteId").value(loteId))
                .andExpect(jsonPath("$.titularSub").value(productor))
                .andExpect(jsonPath("$.lat").value(-31.4201))
                .andExpect(jsonPath("$.hectareas").value(40))
                .andExpect(jsonPath("$.cultivo").value("soja"))
                .andExpect(jsonPath("$.etapa").value("FLORACION"));
    }

    @Test
    void unProductorNoVeNiModificaLotesAjenosYRecibe404() throws Exception {
        long loteAjeno = crearLote("productor-duenio");

        mvc.perform(put("/api/lotes/{id}/cultivo", loteAjeno).with(rol("productor-intruso", "PRODUCTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cultivo\":\"maiz\",\"etapa\":\"SIEMBRA\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RECURSO_NO_ENCONTRADO"));

        mvc.perform(get("/api/lotes/mios").with(rol("productor-intruso", "PRODUCTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void noSePuedeCrearUnLoteEnUnEstablecimientoAjeno() throws Exception {
        long ajeno = crearEstablecimiento("productor-a", "Ajeno");

        mvc.perform(post("/api/establecimientos/{id}/lotes", ajeno).with(rol("productor-b", "PRODUCTOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(LOTE_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void datosInvalidosDevuelven400ConDetalle() throws Exception {
        long establecimientoId = crearEstablecimiento("productor-validacion", "Campo");

        mvc.perform(post("/api/establecimientos/{id}/lotes", establecimientoId)
                        .with(rol("productor-validacion", "PRODUCTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"X\",\"lat\":-95,\"lon\":-64,\"hectareas\":0,"
                                + "\"cultivo\":\"soja\",\"etapa\":\"VEGETATIVO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"))
                .andExpect(jsonPath("$.detalles", hasSize(2)));
    }

    @Test
    void etapaFueraDelCatalogoDevuelve400() throws Exception {
        long loteId = crearLote("productor-catalogo");

        mvc.perform(put("/api/lotes/{id}/cultivo", loteId).with(rol("productor-catalogo", "PRODUCTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cultivo\":\"soja\",\"etapa\":\"COSECHADA\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mvc.perform(get("/api/lotes/mios"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    void unPilotoNoPuedeGestionarLotes() throws Exception {
        mvc.perform(get("/api/lotes/mios").with(rol("piloto-1", "PILOTO")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
    }

    @Test
    void elContextoInternoExigeRolServicio() throws Exception {
        long loteId = crearLote("productor-interno");

        mvc.perform(get("/internal/lotes/{id}/contexto", loteId).with(rol("productor-interno", "PRODUCTOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void contextoDeLoteInexistenteDevuelve404() throws Exception {
        mvc.perform(get("/internal/lotes/{id}/contexto", 999999).with(rol("ms-misiones", "SERVICIO")))
                .andExpect(status().isNotFound());
    }

    private long crearEstablecimiento(String productor, String nombre) throws Exception {
        String cuerpo = mvc.perform(post("/api/establecimientos").with(rol(productor, "PRODUCTOR"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"" + nombre + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(cuerpo, "$.id")).longValue();
    }

    private long crearLote(String productor) throws Exception {
        long establecimientoId = crearEstablecimiento(productor, "Campo de " + productor);
        String cuerpo = mvc.perform(post("/api/establecimientos/{id}/lotes", establecimientoId)
                        .with(rol(productor, "PRODUCTOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(LOTE_JSON))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(cuerpo, "$.id")).longValue();
    }

    private static RequestPostProcessor rol(String sub, String rol) {
        return jwt().jwt(j -> j.subject(sub)).authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }
}
