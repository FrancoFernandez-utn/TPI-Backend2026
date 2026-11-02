package ar.edu.tpi.campos.api.dto;

import ar.edu.tpi.campos.dominio.CultivoLote;
import ar.edu.tpi.campos.dominio.EtapaCultivo;
import java.time.Instant;

public record CultivoActualResponse(String cultivo, EtapaCultivo etapa, Instant desde) {

    public static CultivoActualResponse de(CultivoLote c) {
        return c == null ? null : new CultivoActualResponse(c.getCultivo(), c.getEtapa(), c.getDesde());
    }
}
