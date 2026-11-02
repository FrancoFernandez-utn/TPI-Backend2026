package ar.edu.tpi.misiones.api.dto;

import ar.edu.tpi.misiones.dominio.EstadoPiloto;
import ar.edu.tpi.misiones.dominio.Piloto;
import ar.edu.tpi.misiones.dominio.TipoMision;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.TreeSet;

public record PilotoResponse(
        Long id,
        String usuarioSub,
        String licenciaNro,
        LocalDate licenciaVence,
        BigDecimal horasVuelo,
        EstadoPiloto estado,
        Set<TipoMision> habilitaciones) {

    public static PilotoResponse de(Piloto p) {
        return new PilotoResponse(p.getId(), p.getUsuarioSub(), p.getLicenciaNro(), p.getLicenciaVence(),
                p.getHorasVuelo(), p.getEstado(), new TreeSet<>(p.getHabilitaciones()));
    }
}
