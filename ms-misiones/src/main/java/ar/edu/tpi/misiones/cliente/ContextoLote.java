package ar.edu.tpi.misiones.cliente;

import ar.edu.tpi.misiones.dominio.EtapaCultivo;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Contexto del lote recibido de ms-campos (GET /internal/lotes/{id}/contexto).
 * Es el DTO de ms-misiones para ese contrato: no se comparte codigo con ms-campos.
 */
public record ContextoLote(
        Long loteId,
        String titularSub,
        BigDecimal lat,
        BigDecimal lon,
        BigDecimal hectareas,
        String estado,
        String cultivo,
        EtapaCultivo etapa,
        Instant etapaDesde) {
}
