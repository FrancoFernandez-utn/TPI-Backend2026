package ar.edu.tpi.misiones.cliente;

import ar.edu.tpi.commons.excepcion.RecursoNoEncontradoException;
import ar.edu.tpi.commons.excepcion.ServicioNoDisponibleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Cliente de ms-campos. ms-misiones inicia la colaboracion: necesita el contexto del lote para decidir.
 *
 * <p>Ante un lote inexistente responde 404. Si ms-campos no responde, falla cerrado con 503 y
 * Retry-After: no se autoriza una mision sin conocer el lote (seccion 8.1 del dominio).
 */
public class CamposCliente {

    private static final Logger log = LoggerFactory.getLogger(CamposCliente.class);

    private final RestClient restClient;
    private final long reintentarEnSegundos;

    public CamposCliente(RestClient restClient, long reintentarEnSegundos) {
        this.restClient = restClient;
        this.reintentarEnSegundos = reintentarEnSegundos;
    }

    public ContextoLote contexto(Long loteId) {
        try {
            ContextoLote contexto = restClient.get()
                    .uri("/internal/lotes/{id}/contexto", loteId)
                    .retrieve()
                    .body(ContextoLote.class);
            if (contexto == null) {
                throw new IllegalStateException("ms-campos devolvio un contexto vacio");
            }
            return contexto;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
                throw new RecursoNoEncontradoException("Lote", loteId);
            }
            log.error("ms-campos rechazo la consulta del lote {}: {}", loteId, e.getStatusCode());
            throw new ServicioNoDisponibleException("ms-campos", reintentarEnSegundos);
        } catch (RestClientException | IllegalStateException | OAuth2AuthorizationException e) {
            // Incluye la falla al pedir el token de servicio a Keycloak.
            log.warn("ms-campos no disponible al consultar el lote {}: {}", loteId, e.getMessage());
            throw new ServicioNoDisponibleException("ms-campos", reintentarEnSegundos);
        }
    }
}
