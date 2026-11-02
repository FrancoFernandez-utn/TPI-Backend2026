package ar.edu.tpi.misiones.cliente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import ar.edu.tpi.commons.excepcion.RecursoNoEncontradoException;
import ar.edu.tpi.commons.excepcion.ServicioNoDisponibleException;
import ar.edu.tpi.misiones.dominio.EtapaCultivo;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Contrato con ms-campos y traduccion de sus fallas (seccion 8.1 del dominio). */
class CamposClienteTest {

    private MockRestServiceServer servidor;
    private CamposCliente cliente;

    @BeforeEach
    void configurar() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://ms-campos:8082");
        servidor = MockRestServiceServer.bindTo(builder).build();
        cliente = new CamposCliente(builder.build(), 30);
    }

    @Test
    void leeElContextoDelLote() {
        servidor.expect(requestTo("http://ms-campos:8082/internal/lotes/7/contexto"))
                .andRespond(withSuccess("""
                        {"loteId":7,"titularSub":"p-1","lat":-31.42,"lon":-64.19,"hectareas":40.00,
                         "estado":"ACTIVO","cultivo":"soja","etapa":"FLORACION","etapaDesde":"2030-03-01T12:00:00Z"}""",
                        MediaType.APPLICATION_JSON));

        ContextoLote contexto = cliente.contexto(7L);

        assertThat(contexto.titularSub()).isEqualTo("p-1");
        assertThat(contexto.hectareas()).isEqualByComparingTo("40");
        assertThat(contexto.etapa()).isEqualTo(EtapaCultivo.FLORACION);
    }

    @Test
    void loteInexistenteSeTraduceA404() {
        servidor.expect(requestTo("http://ms-campos:8082/internal/lotes/8/contexto")).andRespond(withResourceNotFound());

        assertThatThrownBy(() -> cliente.contexto(8L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void errorDelServidorFallaCerradoCon503() {
        servidor.expect(requestTo("http://ms-campos:8082/internal/lotes/9/contexto")).andRespond(withServerError());

        assertThatThrownBy(() -> cliente.contexto(9L))
                .isInstanceOf(ServicioNoDisponibleException.class)
                .extracting(e -> ((ServicioNoDisponibleException) e).getReintentarEnSegundos()).isEqualTo(30L);
    }

    @Test
    void tokenDeServicioRechazadoTambienFallaCerrado() {
        servidor.expect(requestTo("http://ms-campos:8082/internal/lotes/10/contexto"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> cliente.contexto(10L)).isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    void sinConexionFallaCerrado() {
        servidor.expect(requestTo("http://ms-campos:8082/internal/lotes/11/contexto")).andRespond(request -> {
            throw new IOException("Connection refused");
        });

        assertThatThrownBy(() -> cliente.contexto(11L)).isInstanceOf(ServicioNoDisponibleException.class);
    }
}
