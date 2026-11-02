package ar.edu.tpi.campos.api.dto;

import ar.edu.tpi.campos.dominio.CultivoLote;
import ar.edu.tpi.campos.dominio.EstadoLote;
import ar.edu.tpi.campos.dominio.EtapaCultivo;
import ar.edu.tpi.campos.dominio.Lote;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Contrato interno con ms-misiones: todo lo que ms-misiones necesita saber del lote
 * para decidir una mision (titularidad, ubicacion, superficie, cultivo y etapa).
 * Solo lo puede pedir un cliente con rol SERVICIO.
 */
public record ContextoLoteResponse(
        Long loteId,
        String titularSub,
        BigDecimal lat,
        BigDecimal lon,
        BigDecimal hectareas,
        EstadoLote estado,
        String cultivo,
        EtapaCultivo etapa,
        Instant etapaDesde) {

    public static ContextoLoteResponse de(Lote lote, CultivoLote actual) {
        return new ContextoLoteResponse(lote.getId(), lote.getTitularSub(), lote.getLat(), lote.getLon(),
                lote.getHectareas(), lote.getEstado(),
                actual == null ? null : actual.getCultivo(),
                actual == null ? null : actual.getEtapa(),
                actual == null ? null : actual.getDesde());
    }
}
