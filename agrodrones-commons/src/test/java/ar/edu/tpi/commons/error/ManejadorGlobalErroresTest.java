package ar.edu.tpi.commons.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.tpi.commons.excepcion.ConflictoException;
import ar.edu.tpi.commons.excepcion.RecursoNoEncontradoException;
import ar.edu.tpi.commons.excepcion.ServicioNoDisponibleException;
import ar.edu.tpi.commons.excepcion.SolicitudInvalidaException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class ManejadorGlobalErroresTest {

    private MockMvc mvc;

    @BeforeEach
    void configurar() {
        LocalValidatorFactoryBean validador = new LocalValidatorFactoryBean();
        validador.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new ControladorDePrueba())
                .setControllerAdvice(new ManejadorGlobalErrores())
                .setValidator(validador)
                .build();
    }

    @Test
    void recursoInexistenteDevuelve404ConFormatoEstandar() throws Exception {
        mvc.perform(get("/prueba/no-encontrado/7"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.codigo").value("RECURSO_NO_ENCONTRADO"))
                .andExpect(jsonPath("$.mensaje").value("Lote 7 no encontrado"))
                .andExpect(jsonPath("$.path").value("/prueba/no-encontrado/7"));
    }

    @Test
    void conflictoDevuelve409ConSuCodigo() throws Exception {
        mvc.perform(get("/prueba/conflicto"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ESTADO_INVALIDO"));
    }

    @Test
    void dependenciaCaidaDevuelve503ConRetryAfter() throws Exception {
        mvc.perform(get("/prueba/caido"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "30"))
                .andExpect(jsonPath("$.codigo").value("DEPENDENCIA_NO_DISPONIBLE"));
    }

    @Test
    void validacionDeCamposDevuelve400ConUnDetallePorCampo() throws Exception {
        mvc.perform(post("/prueba/validar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"hectareas\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"))
                .andExpect(jsonPath("$.detalles.length()").value(2));
    }

    @Test
    void jsonMalFormadoDevuelve400() throws Exception {
        mvc.perform(post("/prueba/validar").contentType(MediaType.APPLICATION_JSON).content("{nombre:"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"));
    }

    @Test
    void solicitudInvalidaDeNegocioDevuelve400() throws Exception {
        mvc.perform(get("/prueba/invalida"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("falta la receta"));
    }

    @Test
    void parametroConTipoIncorrectoDevuelve400() throws Exception {
        mvc.perform(get("/prueba/no-encontrado/abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void accesoDenegadoDevuelve403() throws Exception {
        mvc.perform(get("/prueba/denegado"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
    }

    @Test
    void errorInesperadoDevuelve500SinExponerDetallesInternos() throws Exception {
        mvc.perform(get("/prueba/inesperado"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERROR_INTERNO"))
                .andExpect(jsonPath("$.mensaje").value("Ocurrio un error inesperado"));
    }

    record DatosPrueba(@NotBlank String nombre, @Positive double hectareas) {
    }

    @RestController
    static class ControladorDePrueba {

        @GetMapping("/prueba/no-encontrado/{id}")
        String noEncontrado(@PathVariable Long id) {
            throw new RecursoNoEncontradoException("Lote", id);
        }

        @GetMapping("/prueba/conflicto")
        String conflicto() {
            throw new ConflictoException("ESTADO_INVALIDO", "No se puede cancelar");
        }

        @GetMapping("/prueba/caido")
        String caido() {
            throw new ServicioNoDisponibleException("ms-campos", 30);
        }

        @GetMapping("/prueba/invalida")
        String invalida() {
            throw new SolicitudInvalidaException("falta la receta");
        }

        @GetMapping("/prueba/denegado")
        String denegado() {
            throw new AccessDeniedException("no");
        }

        @GetMapping("/prueba/inesperado")
        String inesperado() {
            throw new IllegalStateException("detalle interno que no debe filtrarse");
        }

        @PostMapping("/prueba/validar")
        String validar(@Valid @RequestBody DatosPrueba datos) {
            return "ok";
        }
    }
}
