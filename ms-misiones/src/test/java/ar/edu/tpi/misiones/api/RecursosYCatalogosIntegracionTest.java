package ar.edu.tpi.misiones.api;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.tpi.misiones.PostgresEmbebido;
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
class RecursosYCatalogosIntegracionTest extends PostgresEmbebido {

    @Autowired
    private MockMvc mvc;

    @Test
    void pilotoRegistraSuPerfilUnaSolaVezYLoConsulta() throws Exception {
        String cuerpo = """
                {"licenciaNro":"ANAC-123","licenciaVence":"2027-12-31","horasVuelo":35.5,
                 "habilitaciones":["PULVERIZACION","MONITOREO"]}""";

        mvc.perform(post("/api/pilotos").with(usuario("piloto-perfil", "PILOTO"))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.usuarioSub").value("piloto-perfil"))
                .andExpect(jsonPath("$.habilitaciones", hasSize(2)));

        mvc.perform(post("/api/pilotos").with(usuario("piloto-perfil", "PILOTO"))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PERFIL_EXISTENTE"));

        mvc.perform(get("/api/pilotos/yo").with(usuario("piloto-perfil", "PILOTO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.licenciaNro").value("ANAC-123"));
    }

    @Test
    void pilotoSinPerfilRecibe404() throws Exception {
        mvc.perform(get("/api/pilotos/yo").with(usuario("piloto-sin-perfil", "PILOTO")))
                .andExpect(status().isNotFound());
    }

    @Test
    void perfilDePilotoSinHabilitacionesEsInvalido() throws Exception {
        mvc.perform(post("/api/pilotos").with(usuario("piloto-x", "PILOTO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"licenciaNro\":\"A\",\"licenciaVence\":\"2027-01-01\",\"horasVuelo\":1,"
                                + "\"habilitaciones\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unProductorNoPuedeRegistrarsePiloto() throws Exception {
        mvc.perform(post("/api/pilotos").with(usuario("productor-1", "PRODUCTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"licenciaNro\":\"A\",\"licenciaVence\":\"2027-01-01\",\"horasVuelo\":1,"
                                + "\"habilitaciones\":[\"MONITOREO\"]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCreaModeloYPilotoRegistraDronQueQuedaOperativo() throws Exception {
        long modeloId = crearModelo("Agras T40-test");

        mvc.perform(post("/api/drones").with(usuario("piloto-dron", "PILOTO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modeloId\":" + modeloId + ",\"matricula\":\"lv-x123\","
                                + "\"registroVence\":\"2027-06-30\",\"seguroVence\":\"2027-06-30\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("OPERATIVO"))
                .andExpect(jsonPath("$.matricula").value("LV-X123"));

        mvc.perform(post("/api/drones").with(usuario("admin", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modeloId\":" + modeloId + ",\"matricula\":\"LV-X123\","
                                + "\"registroVence\":\"2027-06-30\",\"seguroVence\":\"2027-06-30\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("MATRICULA_DUPLICADA"));
    }

    @Test
    void dronConModeloInexistenteDevuelve404() throws Exception {
        mvc.perform(post("/api/drones").with(usuario("admin", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modeloId\":999999,\"matricula\":\"LV-Z\","
                                + "\"registroVence\":\"2027-06-30\",\"seguroVence\":\"2027-06-30\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void soloAdminEscribeProductosPeroCualquierAutenticadoLosLee() throws Exception {
        String producto = """
                {"nombre":"Herbicida Test","intervaloDias":7,"etapasProhibidas":["FLORACION"]}""";

        mvc.perform(post("/api/productos").with(usuario("productor-1", "PRODUCTOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(producto))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/productos").with(usuario("admin", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(producto))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.etapasProhibidas[0]").value("FLORACION"));

        mvc.perform(get("/api/productos").with(usuario("productor-1", "PRODUCTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombre", hasItem("Herbicida Test")));
    }

    @Test
    void losUmbralesInicialesSonLosDelDominioYElAdminLosPuedeReemplazar() throws Exception {
        mvc.perform(get("/api/umbrales").with(usuario("piloto-1", "PILOTO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.tipo=='PULVERIZACION')].umbrales[?(@.variable=='VIENTO')].minimo")
                        .value(3.0))
                .andExpect(jsonPath("$[?(@.tipo=='PULVERIZACION')].umbrales[?(@.variable=='VIENTO')].maximo")
                        .value(15.0));

        String nuevos = """
                {"umbrales":[
                  {"variable":"VIENTO","maximo":18,"maxInclusivo":false},
                  {"variable":"RAFAGA","maximo":28,"maxInclusivo":false}]}""";

        mvc.perform(put("/api/umbrales/SIEMBRA_COBERTURA").with(usuario("admin", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(nuevos))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.umbrales", hasSize(2)));

        mvc.perform(put("/api/umbrales/SIEMBRA_COBERTURA").with(usuario("piloto-1", "PILOTO"))
                        .contentType(MediaType.APPLICATION_JSON).content(nuevos))
                .andExpect(status().isForbidden());
    }

    @Test
    void umbralSinExtremosOConMinimoMayorAlMaximoEsInvalido() throws Exception {
        mvc.perform(put("/api/umbrales/MONITOREO").with(usuario("admin", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"umbrales\":[{\"variable\":\"VIENTO\",\"minimo\":10,\"maximo\":5}]}"))
                .andExpect(status().isBadRequest());

        mvc.perform(put("/api/umbrales/MONITOREO").with(usuario("admin", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"umbrales\":[{\"variable\":\"VIENTO\"}]}"))
                .andExpect(status().isBadRequest());

        mvc.perform(put("/api/umbrales/MONITOREO").with(usuario("admin", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"umbrales\":[{\"variable\":\"VIENTO\",\"maximo\":5},"
                                + "{\"variable\":\"VIENTO\",\"maximo\":6}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tipoDeMisionInexistenteEnLaRutaDevuelve400() throws Exception {
        mvc.perform(put("/api/umbrales/FUMIGACION").with(usuario("admin", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"umbrales\":[]}"))
                .andExpect(status().isBadRequest());
    }

    private long crearModelo(String nombre) throws Exception {
        String cuerpo = mvc.perform(post("/api/modelos-dron").with(usuario("admin", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"" + nombre + "\",\"capacidadL\":40,\"haPorHora\":10,"
                                + "\"haPorVuelo\":5,\"limiteRafagaKmh\":35,\"tieneCamara\":true}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(cuerpo, "$.id")).longValue();
    }

    private static RequestPostProcessor usuario(String sub, String rol) {
        return jwt().jwt(j -> j.subject(sub)).authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }
}
