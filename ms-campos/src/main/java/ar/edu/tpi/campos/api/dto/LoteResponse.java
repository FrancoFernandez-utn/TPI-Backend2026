package ar.edu.tpi.campos.api.dto;

import ar.edu.tpi.campos.dominio.CultivoLote;
import ar.edu.tpi.campos.dominio.EstadoLote;
import ar.edu.tpi.campos.dominio.Lote;
import java.math.BigDecimal;

public record LoteResponse(
        Long id,
        Long establecimientoId,
        String establecimientoNombre,
        String nombre,
        BigDecimal lat,
        BigDecimal lon,
        BigDecimal hectareas,
        EstadoLote estado,
        CultivoActualResponse cultivoActual) {

    public static LoteResponse de(Lote lote, CultivoLote cultivoActual) {
        return new LoteResponse(lote.getId(), lote.getEstablecimiento().getId(),
                lote.getEstablecimiento().getNombre(), lote.getNombre(), lote.getLat(), lote.getLon(),
                lote.getHectareas(), lote.getEstado(), CultivoActualResponse.de(cultivoActual));
    }
}
